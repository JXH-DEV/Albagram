package com.albagram.app.data.seed

import com.albagram.app.domain.crossword.CrosswordGridBuilder
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedContentIntegrityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun allSeedLinksResolveAndCrosswordsBuild() {
        val words = readList<SeedWord>("words.json")
        val crosswords = readAsset<SeedCrosswordFile>("crosswords.json")
        val quiz = readList<SeedQuizQuestion>("quiz.json")
        val essays = readList<SeedEssayPrompt>("essays.json")

        val ids = words.map { it.wordId }.toSet()
        assertEquals("wordIds must be unique", words.size, ids.size)
        assertTrue("word corpus should be rich enough for launch", words.size >= 180)
        words.forEach { word ->
            assertTrue("term should not be blank for ${word.wordId}", word.term.isNotBlank())
            assertTrue("definition should not be blank for ${word.wordId}", word.definition.isNotBlank())
            word.synonyms.forEach { ref ->
                assertTrue("synonym ref must exist for ${word.wordId}: $ref", ref in ids)
                assertFalse("self synonym not allowed for ${word.wordId}", ref == word.wordId)
            }
            word.antonyms.forEach { ref ->
                assertTrue("antonym ref must exist for ${word.wordId}: $ref", ref in ids)
                assertFalse("self antonym not allowed for ${word.wordId}", ref == word.wordId)
            }
        }

        quiz.forEach { q ->
            assertTrue("quiz options should have at least two", q.options.size >= 2)
            assertTrue("correct index must be valid", q.correctIndex in q.options.indices)
            assertTrue("quiz prompt should not be blank: ${q.questionId}", q.prompt.isNotBlank())
            q.relatedWordId?.let { id ->
                assertTrue("quiz relatedWordId must exist: $id", id in ids)
            }
        }

        essays.forEach { essay ->
            essay.suggestedWordIds.forEach { id ->
                assertTrue("essay suggestedWordId must exist: $id", id in ids)
            }
        }

        crosswords.puzzles.forEach { puzzle ->
            puzzle.clues.forEach { clue ->
                assertTrue("crossword clue wordId must exist: ${clue.wordId}", clue.wordId in ids)
                assertTrue("crossword clue text should not be blank: ${clue.clueId}", clue.clueText.isNotBlank())
                assertEquals(
                    "crossword answer length mismatch for ${clue.clueId}",
                    clue.length,
                    clue.answer.length
                )
            }
            val cells = CrosswordGridBuilder.buildCells(puzzle.width, puzzle.height, puzzle.clues)
            assertTrue("puzzle must have open cells: ${puzzle.puzzleId}", cells.any { !it.block })
        }
    }

    private inline fun <reified T> readAsset(fileName: String): T {
        val text = assetPath(fileName).readText(Charsets.UTF_8)
        return json.decodeFromString(text)
    }

    private inline fun <reified T> readList(fileName: String): List<T> {
        val text = assetPath(fileName).readText(Charsets.UTF_8)
        return json.decodeFromString(text)
    }

    private fun assetPath(fileName: String): File {
        val root = File(requireNotNull(System.getProperty("user.dir")))
        val fromRoot = File(root, "app/src/main/assets/seed/$fileName")
        if (fromRoot.exists()) return fromRoot
        val fromApp = File(root, "src/main/assets/seed/$fileName")
        if (fromApp.exists()) return fromApp
        error("Missing seed asset file: $fileName")
    }
}
