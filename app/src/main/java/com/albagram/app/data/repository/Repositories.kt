package com.albagram.app.data.repository

import com.albagram.app.data.local.dao.CrosswordDao
import com.albagram.app.data.local.dao.CrosswordProgressDao
import com.albagram.app.data.local.dao.EssayDao
import com.albagram.app.data.local.dao.ProgressDao
import com.albagram.app.data.local.dao.QuizDao
import com.albagram.app.data.local.dao.SavedWordDao
import com.albagram.app.data.local.dao.WordDao
import com.albagram.app.data.local.entity.CrosswordClueEntity
import com.albagram.app.data.local.entity.CrosswordProgressEntity
import com.albagram.app.data.local.entity.CrosswordPuzzleEntity
import com.albagram.app.data.local.entity.EssayDraftEntity
import com.albagram.app.data.local.entity.EssayPromptEntity
import com.albagram.app.data.local.entity.QuizQuestionEntity
import com.albagram.app.data.local.entity.SavedWordEntity
import com.albagram.app.data.local.entity.UserProgressEntity
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.domain.crossword.ClueDirection
import com.albagram.app.domain.crossword.CrosswordClue
import com.albagram.app.domain.daily.DailyChallengeKeys
import com.albagram.app.domain.progress.ProgressMilestones
import com.albagram.app.domain.rhyme.RhymeMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao,
    private val json: Json
) {
    fun observeGlossary() = wordDao.observeGlossary()
    fun observeByLetter(letter: String) = wordDao.observeByLetter(letter)
    fun search(query: String) = wordDao.search(query)
    fun observeById(id: String) = wordDao.observeById(id)
    suspend fun getById(id: String) = wordDao.getById(id)
    suspend fun getByIds(ids: List<String>) = wordDao.getByIds(ids)
    suspend fun getAllGlossary() = wordDao.getAllGlossary()

    fun parseIds(jsonArray: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(jsonArray) }.getOrDefault(emptyList())

    suspend fun upsertRemoteEntry(
        term: String,
        definition: String,
        partOfSpeech: String?,
        sourceLabel: String,
        sourceUrl: String
    ): String {
        val normalizedTerm = RhymeMatcher.normalize(term)
        val slug = normalizedTerm
            .filter { it.isLetterOrDigit() }
            .take(40)
            .ifBlank { "entry" }
        val suffix = normalizedTerm.hashCode().toUInt().toString(36)
        val wordId = "remote-$slug-$suffix"
        val rawLetter = term.trim().firstOrNull()?.uppercaseChar()
        val letter = if (rawLetter == 'Ë') "E" else rawLetter?.toString() ?: "?"
        wordDao.insertAll(
            listOf(
                WordEntity(
                    wordId = wordId,
                    term = term,
                    definition = definition,
                    partOfSpeech = partOfSpeech,
                    letterOfAlphabet = letter,
                    rhymeKey = RhymeMatcher.rhymeKeyFor(term),
                    inGlossary = false,
                    usageNote = "Burim: $sourceLabel\n$sourceUrl"
                )
            )
        )
        return wordId
    }
}

@Singleton
class BookRepository @Inject constructor(
    private val savedWordDao: SavedWordDao
) {
    fun observeSaved(query: String) = savedWordDao.observeSavedWords(query)
    fun observeIsSaved(wordId: String) = savedWordDao.observeIsSaved(wordId)
    fun observeCount() = savedWordDao.observeCount()

    suspend fun save(wordId: String, sourceModule: String?) {
        val existing = savedWordDao.get(wordId)
        savedWordDao.upsert(
            SavedWordEntity(
                wordId = wordId,
                dateSaved = existing?.dateSaved ?: System.currentTimeMillis(),
                timesReviewed = existing?.timesReviewed ?: 0,
                sourceModule = sourceModule ?: existing?.sourceModule
            )
        )
    }

    suspend fun remove(wordId: String) = savedWordDao.delete(wordId)

    suspend fun markReviewed(wordId: String) {
        if (savedWordDao.get(wordId) != null) {
            savedWordDao.incrementReviewed(wordId)
        }
    }
}

@Singleton
class CrosswordRepository @Inject constructor(
    private val crosswordDao: CrosswordDao,
    private val progressDao: CrosswordProgressDao,
    private val json: Json
) {
    fun observePuzzles(): Flow<List<CrosswordPuzzleEntity>> = crosswordDao.observePuzzles()
    fun observeProgress() = progressDao.observeAll()

    suspend fun getLatestInProgress() = progressDao.getLatestInProgress()

    suspend fun countCompleted() = progressDao.countCompleted()

    suspend fun countPuzzles() = crosswordDao.count()

    suspend fun getPuzzle(id: String) = crosswordDao.getPuzzle(id)

    suspend fun getClues(puzzleId: String): List<CrosswordClue> {
        return crosswordDao.getClues(puzzleId)
            .map { it.toDomain() }
            .filter { clue ->
                clue.length > 0 &&
                    clue.answer.isNotBlank() &&
                    clue.answer.length == clue.length &&
                    clue.clueText.isNotBlank()
            }
    }

    suspend fun getProgress(puzzleId: String) = progressDao.get(puzzleId)

    suspend fun saveProgress(
        puzzleId: String,
        entries: Map<Pair<Int, Int>, Char>,
        solvedClueIds: Set<String>,
        hintedClueIds: Set<String>,
        completed: Boolean
    ) {
        val map = entries.mapKeys { "${it.key.first},${it.key.second}" }
            .mapValues { it.value.toString() }
        progressDao.upsert(
            CrosswordProgressEntity(
                puzzleId = puzzleId,
                entriesJson = json.encodeToString(map),
                solvedClueIdsJson = json.encodeToString(solvedClueIds.toList()),
                hintedClueIdsJson = json.encodeToString(hintedClueIds.toList()),
                completed = completed,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun decodeEntries(progress: CrosswordProgressEntity): Map<Pair<Int, Int>, Char> {
        val raw = runCatching {
            json.decodeFromString<Map<String, String>>(progress.entriesJson)
        }.getOrDefault(emptyMap())
        return raw.mapNotNull { (k, v) ->
            val parts = k.split(",")
            if (parts.size != 2) return@mapNotNull null
            val r = parts[0].toIntOrNull() ?: return@mapNotNull null
            val c = parts[1].toIntOrNull() ?: return@mapNotNull null
            val ch = v.firstOrNull() ?: return@mapNotNull null
            (r to c) to ch
        }.toMap()
    }

    fun decodeIdSet(jsonArray: String): Set<String> =
        runCatching { json.decodeFromString<List<String>>(jsonArray).toSet() }
            .getOrDefault(emptySet())

    private fun CrosswordClueEntity.toDomain() = CrosswordClue(
        clueId = clueId,
        wordId = wordId,
        direction = if (direction.equals("DOWN", true)) ClueDirection.DOWN else ClueDirection.ACROSS,
        number = number,
        row = row,
        col = col,
        length = length,
        clueText = clueText,
        answer = answer
    )
}

@Singleton
class RhymeRepository @Inject constructor(
    private val wordDao: WordDao
) {
    suspend fun findRhymes(input: String): List<WordEntity> {
        val term = input.trim()
        if (term.isEmpty()) return emptyList()

        val key = RhymeMatcher.rhymeKeyFor(term)
        val byKey = if (key.isNotEmpty()) {
            wordDao.findByRhymeKey(key, term)
        } else {
            emptyList()
        }

        val bySuffix = RhymeMatcher.suffixesToTry(term)
            .flatMap { suffix -> wordDao.findBySuffix(suffix, term) }

        return (byKey + bySuffix)
            .distinctBy { it.wordId }
            .sortedBy { it.term.lowercase() }
    }
}

@Singleton
class EssayRepository @Inject constructor(
    private val essayDao: EssayDao,
    private val wordDao: WordDao,
    private val json: Json
) {
    private val draftMutex = Mutex()

    fun observePrompts() = essayDao.observePrompts()
    suspend fun getPrompt(id: String) = essayDao.getPrompt(id)
    fun observeDraft(promptId: String) = essayDao.observeDraft(promptId)
    suspend fun getDraft(promptId: String) = essayDao.getDraft(promptId)

    suspend fun saveDraft(promptId: String, body: String) = draftMutex.withLock {
        val existing = essayDao.getDraft(promptId)
        essayDao.upsertDraft(
            EssayDraftEntity(
                promptId = promptId,
                body = body,
                updatedAt = System.currentTimeMillis(),
                completedAt = existing?.completedAt
            )
        )
    }

    suspend fun submitEssay(promptId: String, body: String) = draftMutex.withLock {
        val now = System.currentTimeMillis()
        essayDao.upsertDraft(
            EssayDraftEntity(
                promptId = promptId,
                body = body,
                updatedAt = now,
                completedAt = now
            )
        )
    }

    suspend fun countCompleted() = essayDao.countCompleted()

    suspend fun getLatestInProgressDraft() = essayDao.getLatestInProgressDraft()

    suspend fun isCompleted(promptId: String): Boolean =
        essayDao.getDraft(promptId)?.completedAt != null

    fun observeAllDrafts() = essayDao.observeAllDrafts()

    suspend fun countPrompts() = essayDao.count()

    suspend fun suggestedWords(prompt: EssayPromptEntity): List<WordEntity> {
        val ids = runCatching { json.decodeFromString<List<String>>(prompt.suggestedWordIdsJson) }
            .getOrDefault(emptyList())
        return wordDao.getByIds(ids)
    }
}

@Singleton
class QuizRepository @Inject constructor(
    private val quizDao: QuizDao,
    private val json: Json
) {
    suspend fun getShuffledQuestions(): List<QuizQuestionEntity> {
        return quizDao.getAll()
            .filter { q ->
                val options = parseOptions(q)
                options.size >= 2 &&
                    q.correctIndex in options.indices &&
                    q.timeLimitSeconds > 0
            }
            .shuffled()
    }

    fun parseOptions(question: QuizQuestionEntity): List<String> {
        return runCatching {
            json.decodeFromString<List<String>>(question.optionsJson)
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    suspend fun getDailyQuestions(ids: List<String>): List<QuizQuestionEntity> {
        if (ids.isEmpty()) return emptyList()
        return quizDao.getByIds(ids).sortedBy { ids.indexOf(it.questionId) }
    }

    fun isQuestionUsable(question: QuizQuestionEntity): Boolean {
        val options = parseOptions(question)
        return options.size >= 2 &&
            question.correctIndex in options.indices &&
            question.prompt.isNotBlank() &&
            question.timeLimitSeconds > 0
    }
}

@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val json: Json
) {
    private val mutex = Mutex()
    private val _badgeUnlocks = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val badgeUnlocks: SharedFlow<String> = _badgeUnlocks.asSharedFlow()

    @Serializable
    private data class DailyCompletedPayload(
        val date: String = "",
        val keys: List<String> = emptyList()
    )

    fun observe() = progressDao.observe()

    private suspend fun ensureRow() {
        if (progressDao.get() == null) {
            progressDao.upsert(UserProgressEntity())
        }
    }

    suspend fun addScore(points: Int) = mutex.withLock {
        ensureRow()
        progressDao.addScore(points)
        awardMilestoneBadgesLocked()
    }

    suspend fun bumpStreak() = mutex.withLock {
        ensureRow()
        progressDao.bumpStreak()
        awardMilestoneBadgesLocked()
    }

    suspend fun resetStreak() = mutex.withLock {
        ensureRow()
        progressDao.resetStreak()
    }

    suspend fun awardBadge(badge: String) = mutex.withLock {
        ensureRow()
        addBadgeLocked(badge)
    }

    suspend fun recordDailyVisit(today: LocalDate = LocalDate.now()) = mutex.withLock {
        ensureRow()
        val current = progressDao.get() ?: return@withLock
        val todayStr = today.toString()
        if (current.lastVisitDate == todayStr) return@withLock
        val streak = when {
            current.lastVisitDate.isBlank() -> 1
            else -> {
                val last = runCatching { LocalDate.parse(current.lastVisitDate) }.getOrNull()
                if (last == null) 1
                else {
                    val gap = ChronoUnit.DAYS.between(last, today)
                    when {
                        gap <= 0L -> current.dailyVisitStreak
                        gap == 1L -> current.dailyVisitStreak + 1
                        else -> 1
                    }
                }
            }
        }
        progressDao.updateDailyVisit(todayStr, streak)
    }

    suspend fun markDailyComplete(key: String, today: LocalDate = LocalDate.now()) = mutex.withLock {
        ensureRow()
        val current = progressDao.get() ?: return@withLock
        val todayStr = today.toString()
        val payload = parseDailyPayload(current.dailyCompletedJson)
        val keys = if (payload.date == todayStr) payload.keys.toMutableSet() else mutableSetOf()
        keys += key
        progressDao.updateDailyCompleted(
            json.encodeToString(DailyCompletedPayload(date = todayStr, keys = keys.toList()))
        )
        val completedCount = keys.size
        val allComplete = DailyChallengeKeys.all.all { it in keys }
        val newBadges = ProgressMilestones.dailyCompletionBadges(completedCount, allComplete)
        newBadges.forEach { addBadgeLocked(it) }
    }

    fun dailyCompletedKeys(progress: UserProgressEntity, today: LocalDate = LocalDate.now()): Set<String> {
        val payload = parseDailyPayload(progress.dailyCompletedJson)
        return if (payload.date == today.toString()) payload.keys.toSet() else emptySet()
    }

    private fun parseDailyPayload(raw: String): DailyCompletedPayload =
        runCatching { json.decodeFromString<DailyCompletedPayload>(raw) }
            .getOrDefault(DailyCompletedPayload())

    fun parseBadges(progress: UserProgressEntity): List<String> {
        return runCatching { json.decodeFromString<List<String>>(progress.badgesJson) }
            .getOrDefault(emptyList())
    }

    private suspend fun addBadgeLocked(badge: String) {
        val current = progressDao.get() ?: return
        val badges = parseBadges(current).toMutableList()
        if (badge !in badges) {
            badges += badge
            progressDao.updateBadges(json.encodeToString(badges))
            _badgeUnlocks.tryEmit(badge)
        }
    }

    private suspend fun awardMilestoneBadgesLocked() {
        val current = progressDao.get() ?: return
        ProgressMilestones.milestoneBadges(current.score, current.streak).forEach { badge ->
            addBadgeLocked(badge)
        }
    }
}
