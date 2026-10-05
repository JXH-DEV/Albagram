package com.albagram.app.domain.daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyChallengeEngineTest {
    @Test
    fun sameDate_samePicks() {
        val date = LocalDate.of(2026, 8, 23)
        val ids = listOf("a", "b", "c", "d", "e")
        val w1 = DailyChallengeEngine.dailyWordId(ids, date)
        val w2 = DailyChallengeEngine.dailyWordId(ids, date)
        assertEquals(w1, w2)
    }

    @Test
    fun differentDates_canDiffer() {
        val ids = (1..20).map { "id$it" }
        val d1 = LocalDate.of(2026, 1, 1)
        val d2 = LocalDate.of(2026, 6, 15)
        val picks = setOf(
            DailyChallengeEngine.dailyWordId(ids, d1),
            DailyChallengeEngine.dailyWordId(ids, d2)
        )
        assertTrue(picks.size >= 1)
        assertNotEquals(
            DailyChallengeEngine.dailyQuizIds(ids, d1),
            DailyChallengeEngine.dailyQuizIds(ids, d2)
        )
    }

    @Test
    fun dailyQuiz_returnsFive() {
        val ids = (1..10).map { "q$it" }
        val quiz = DailyChallengeEngine.dailyQuizIds(ids, LocalDate.of(2026, 3, 3))
        assertEquals(5, quiz.size)
    }

    @Test
    fun weeklyWords_returnsDistinctUpToFive() {
        val ids = (1..30).map { "w$it" }
        val weekly = DailyChallengeEngine.weeklyWordIds(ids, LocalDate.of(2026, 8, 23))
        assertTrue(weekly.size <= 5)
        assertEquals(weekly.size, weekly.distinct().size)
    }

    @Test
    fun weeklyWords_stableAcrossYearBoundaryWeek() {
        val ids = (1..30).map { "w$it" }
        val d1 = LocalDate.of(2025, 12, 29)
        val d2 = LocalDate.of(2026, 1, 1)
        assertEquals(
            DailyChallengeEngine.weeklyWordIds(ids, d1),
            DailyChallengeEngine.weeklyWordIds(ids, d2)
        )
    }
}
