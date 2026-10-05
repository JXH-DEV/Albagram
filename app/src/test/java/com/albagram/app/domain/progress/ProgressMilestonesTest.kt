package com.albagram.app.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMilestonesTest {
    @Test
    fun nextScoreGoal_cyclesByStep() {
        val g1 = ProgressMilestones.nextScoreGoal(score = 0, step = 50)
        assertEquals(0, g1.current)
        assertEquals(50, g1.target)

        val g2 = ProgressMilestones.nextScoreGoal(score = 63, step = 50)
        assertEquals(13, g2.current)
        assertEquals(50, g2.target)
        assertEquals(37, g2.remaining)
    }

    @Test
    fun nextStreakGoal_picksClosestMilestone() {
        val g = ProgressMilestones.nextStreakGoal(6)
        assertEquals(6, g.current)
        assertEquals(7, g.target)
        assertEquals(1, g.remaining)
    }

    @Test
    fun milestoneBadges_includeScoreAndStreak() {
        val badges = ProgressMilestones.milestoneBadges(score = 320, streak = 8)
        assertTrue("100 pikë" in badges)
        assertTrue("250 pikë" in badges)
        assertTrue("Seri 3" in badges)
        assertTrue("Seri 7" in badges)
    }

    @Test
    fun dailyCompletionBadges_awardActiveDayAndFullCompletion() {
        val partial = ProgressMilestones.dailyCompletionBadges(completedCount = 2, allComplete = false)
        assertTrue("Dita aktive" in partial)
        val full = ProgressMilestones.dailyCompletionBadges(completedCount = 4, allComplete = true)
        assertTrue("Sot 4/4" in full)
    }
}
