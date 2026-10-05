package com.albagram.app.domain.crossword

import com.albagram.app.data.seed.DatabaseSeeder
import com.albagram.app.data.seed.SeedClue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrosswordEngineTest {

    private val clues = listOf(
        SeedClue("1", "balade", "DOWN", 1, 0, 1, 6, "clue", "BALADE"),
        SeedClue("2", "libri", "ACROSS", 2, 2, 1, 5, "clue", "LIBRI"),
        SeedClue("3", "rima", "DOWN", 3, 2, 4, 4, "clue", "RIME"),
        SeedClue("4", "ese", "ACROSS", 4, 5, 1, 3, "clue", "ESE")
    )

    @Test
    fun gridBuilder_acceptsIntersectingPuzzle() {
        val cells = CrosswordGridBuilder.buildCells(6, 6, clues)
        val open = cells.filter { !it.block }
        assertTrue(open.isNotEmpty())
        val libL = cells.first { it.row == 2 && it.col == 1 }
        assertEquals("L", libL.letter)
        val balL = cells.first { it.row == 2 && it.col == 1 }
        assertEquals(libL.letter, balL.letter)
    }

    @Test
    fun engine_validatesCompleteClue() {
        val clue = CrosswordClue(
            clueId = "2",
            wordId = "libri",
            direction = ClueDirection.ACROSS,
            number = 2,
            row = 2,
            col = 1,
            length = 5,
            clueText = "x",
            answer = "LIBRI"
        )
        val entries = mapOf(
            (2 to 1) to 'L',
            (2 to 2) to 'I',
            (2 to 3) to 'B',
            (2 to 4) to 'R',
            (2 to 5) to 'I'
        )
        assertTrue(CrosswordEngine.isClueCorrect(clue, entries))
    }

    @Test
    fun allSeedPuzzles_buildWithoutConflict() {
        val puzzles = listOf(
            6 to listOf(
                SeedClue("gjuha_1d", "balade", "DOWN", 1, 0, 1, 6, "c", "BALADE"),
                SeedClue("gjuha_2a", "libri", "ACROSS", 2, 2, 1, 5, "c", "LIBRI"),
                SeedClue("gjuha_3d", "rima", "DOWN", 3, 2, 4, 4, "c", "RIME"),
                SeedClue("gjuha_4a", "ese", "ACROSS", 4, 5, 1, 3, "c", "ESE")
            ),
            7 to listOf(
                SeedClue("poezi_1a", "poezi", "ACROSS", 1, 0, 0, 5, "c", "POEZI"),
                SeedClue("poezi_2d", "ide", "DOWN", 2, 0, 4, 3, "c", "IDE"),
                SeedClue("poezi_3d", "libri", "DOWN", 3, 0, 6, 5, "c", "LIBRI"),
                SeedClue("poezi_4a", "fjale", "ACROSS", 4, 2, 0, 5, "c", "FJALE")
            ),
            5 to listOf(
                SeedClue("shkolla_1d", "shkolla", "DOWN", 1, 0, 2, 7, "c", "SHKOLLA"),
                SeedClue("shkolla_2a", "molla", "ACROSS", 2, 4, 0, 5, "c", "MOLLA")
            ),
            6 to listOf(
                SeedClue("natyra_1d", "dielli", "DOWN", 1, 0, 2, 6, "c", "DIELLI"),
                SeedClue("natyra_2a", "deti", "ACROSS", 2, 0, 2, 4, "c", "DETI"),
                SeedClue("natyra_3a", "mali", "ACROSS", 3, 3, 0, 4, "c", "MALI"),
                SeedClue("natyra_4a", "lumi", "ACROSS", 4, 4, 2, 4, "c", "LUMI")
            ),
            5 to listOf(
                SeedClue("shkrimi_1a", "libri", "ACROSS", 1, 0, 0, 5, "c", "LIBRI"),
                SeedClue("shkrimi_2d", "rima", "DOWN", 2, 0, 3, 4, "c", "RIME"),
                SeedClue("shkrimi_3a", "stil", "ACROSS", 3, 1, 1, 4, "c", "STIL"),
                SeedClue("shkrimi_4a", "tema", "ACROSS", 4, 2, 1, 4, "c", "TEME"),
                SeedClue("shkrimi_5d", "ese", "DOWN", 5, 2, 2, 3, "c", "ESE")
            ),
            6 to listOf(
                SeedClue("atdheu_1a", "atdheu", "ACROSS", 1, 0, 0, 6, "c", "ATDHEU"),
                SeedClue("atdheu_2d", "deti", "DOWN", 2, 0, 2, 4, "c", "DETI"),
                SeedClue("atdheu_3a", "era", "ACROSS", 3, 1, 2, 3, "c", "ERA"),
                SeedClue("atdheu_4a", "liria", "ACROSS", 4, 3, 1, 5, "c", "LIRIA")
            )
        )
        val heights = listOf(6, 5, 7, 6, 5, 5)
        puzzles.forEachIndexed { index, (width, seedClues) ->
            val cells = CrosswordGridBuilder.buildCells(width, heights[index], seedClues)
            assertTrue("puzzle $index should have open cells", cells.any { !it.block })
        }
    }

    @Test
    fun shouldAwardScore_skipsAlreadySolved() {
        assertTrue(CrosswordPlayRules.shouldAwardScore("a", emptySet()))
        assertFalse(CrosswordPlayRules.shouldAwardScore("a", setOf("a")))
    }

    @Test
    fun scoreForClue_zeroWhenHinted() {
        assertEquals(10, CrosswordPlayRules.scoreForClue("c1", emptySet()))
        assertEquals(0, CrosswordPlayRules.scoreForClue("c1", setOf("c1")))
    }

    @Test
    fun isCellLocked_whenCoveredBySolvedClue() {
        val across = CrosswordClue("a", "w", ClueDirection.ACROSS, 1, 2, 1, 5, "c", "LIBRI")
        val down = CrosswordClue("d", "w", ClueDirection.DOWN, 1, 0, 1, 6, "c", "BALADE")
        val clues = listOf(across, down)
        assertFalse(CrosswordPlayRules.isCellLocked(2, 1, clues, emptySet()))
        assertTrue(CrosswordPlayRules.isCellLocked(2, 1, clues, setOf("a")))
        assertTrue(CrosswordPlayRules.isCellLocked(2, 1, clues, setOf("d")))
        assertFalse(CrosswordPlayRules.isCellLocked(5, 5, clues, setOf("a")))
    }

    @Test
    fun intersectionRetap_togglesDirection() {
        val across = CrosswordClue("a", "w", ClueDirection.ACROSS, 1, 2, 1, 5, "c", "LIBRI")
        val down = CrosswordClue("d", "w", ClueDirection.DOWN, 1, 0, 1, 6, "c", "BALADE")
        val covering = listOf(across, down)
        val toggled = CrosswordPlayRules.resolveClueOnCellTap(covering, across)
        assertEquals(down.clueId, toggled?.clueId)
        val toggledBack = CrosswordPlayRules.resolveClueOnCellTap(covering, toggled)
        assertEquals(across.clueId, toggledBack?.clueId)
    }

    @Test
    fun seedVersion_isLaunchPack() {
        assertEquals(3, DatabaseSeeder.SEED_VERSION)
    }
}
