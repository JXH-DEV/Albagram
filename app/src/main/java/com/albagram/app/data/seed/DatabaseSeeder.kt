package com.albagram.app.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.albagram.app.data.local.AppDatabase
import com.albagram.app.data.local.entity.CrosswordClueEntity
import com.albagram.app.data.local.entity.CrosswordPuzzleEntity
import com.albagram.app.data.local.entity.EssayPromptEntity
import com.albagram.app.data.local.entity.QuizQuestionEntity
import com.albagram.app.data.local.entity.UserProgressEntity
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.domain.crossword.CrosswordGridBuilder
import com.albagram.app.domain.rhyme.RhymeMatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val json: Json
) {
    companion object {
        const val SEED_VERSION = 3
        private const val PREFS = "albagram_seed"
        private const val KEY_VERSION = "seed_version"
    }

    private val seedMutex = Mutex()

    suspend fun seedIfNeeded() {
        seedMutex.withLock {
            withContext(Dispatchers.IO) { seed() }
        }
    }

    private suspend fun seed() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val installedVersion = prefs.getInt(KEY_VERSION, 0)
        val catalogEmpty = db.wordDao().count() == 0 ||
            db.crosswordDao().count() == 0 ||
            db.quizDao().count() == 0 ||
            db.essayDao().count() == 0
        val needsReseed = catalogEmpty || installedVersion < SEED_VERSION

        if (!needsReseed) {
            ensureProgress()
            return
        }

        db.withTransaction {
            db.wordDao().clear()
            db.crosswordDao().clearClues()
            db.crosswordDao().clearPuzzles()
            db.quizDao().clear()
            db.essayDao().clearPrompts()

            val words = readList<SeedWord>("seed/words.json").map { it.toEntity(json) }
            db.wordDao().insertAll(words)
            val validWordIds = words.map { it.wordId }.toSet()

            val crosswordFile = readAsset<SeedCrosswordFile>("seed/crosswords.json")
            val puzzles = mutableListOf<CrosswordPuzzleEntity>()
            val clues = mutableListOf<CrosswordClueEntity>()
            for (puzzle in crosswordFile.puzzles) {
                val validClues = puzzle.clues.filter { clue ->
                    clue.wordId in validWordIds &&
                        clue.length > 0 &&
                        clue.answer.isNotBlank()
                }
                if (validClues.isEmpty()) continue
                val gridJson = runCatching {
                    CrosswordGridBuilder.encodeGrid(
                        puzzle.width,
                        puzzle.height,
                        validClues,
                        json
                    )
                }.getOrNull() ?: continue
                puzzles += CrosswordPuzzleEntity(
                    puzzleId = puzzle.puzzleId,
                    title = puzzle.title,
                    width = puzzle.width,
                    height = puzzle.height,
                    gridJson = gridJson
                )
                for (clue in validClues) {
                    clues += CrosswordClueEntity(
                        clueId = clue.clueId,
                        puzzleId = puzzle.puzzleId,
                        wordId = clue.wordId,
                        direction = clue.direction.uppercase(),
                        number = clue.number,
                        row = clue.row,
                        col = clue.col,
                        length = clue.length,
                        clueText = clue.clueText,
                        answer = clue.answer.uppercase()
                    )
                }
            }
            db.crosswordDao().insertPuzzles(puzzles)
            db.crosswordDao().insertClues(clues)

            val quiz = readList<SeedQuizQuestion>("seed/quiz.json").map {
                val sanitizedOptions = it.options.map(String::trim).filter(String::isNotBlank)
                if (
                    sanitizedOptions.size < 2 ||
                    it.correctIndex !in sanitizedOptions.indices ||
                    it.timeLimitSeconds <= 0
                ) {
                    null
                } else {
                    QuizQuestionEntity(
                        questionId = it.questionId,
                        prompt = it.prompt.trim(),
                        optionsJson = json.encodeToString(sanitizedOptions),
                        correctIndex = it.correctIndex,
                        timeLimitSeconds = it.timeLimitSeconds,
                        relatedWordId = it.relatedWordId?.takeIf { id -> id in validWordIds }
                    )
                }
            }.filterNotNull()
            db.quizDao().insertAll(quiz)

            val essays = readList<SeedEssayPrompt>("seed/essays.json").map {
                val suggested = it.suggestedWordIds.filter { id -> id in validWordIds }.distinct()
                EssayPromptEntity(
                    promptId = it.promptId,
                    title = it.title,
                    promptText = it.promptText,
                    suggestedWordIdsJson = json.encodeToString(suggested)
                )
            }
            db.essayDao().insertPrompts(essays)

            ensureProgress()
        }

        prefs.edit().putInt(KEY_VERSION, SEED_VERSION).apply()
    }

    private suspend fun ensureProgress() {
        if (db.progressDao().get() == null) {
            db.progressDao().upsert(UserProgressEntity())
        }
    }

    private inline fun <reified T> readAsset(path: String): T {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }
}

private fun SeedWord.toEntity(json: Json) = WordEntity(
    wordId = wordId,
    term = term,
    definition = definition,
    partOfSpeech = partOfSpeech,
    exampleSentence = exampleSentence,
    letterOfAlphabet = letterOfAlphabet,
    rhymeKey = RhymeMatcher.rhymeKeyFor(term),
    inGlossary = inGlossary,
    synonymsJson = json.encodeToString(synonyms),
    antonymsJson = json.encodeToString(antonyms),
    usageNote = usageNote
)
