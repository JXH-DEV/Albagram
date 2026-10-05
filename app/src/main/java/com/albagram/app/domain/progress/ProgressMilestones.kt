package com.albagram.app.domain.progress

import com.albagram.app.domain.daily.DailyChallengeKeys

data class GoalProgress(
    val current: Int,
    val target: Int
) {
    val remaining: Int get() = (target - current).coerceAtLeast(0)
    val completed: Boolean get() = current >= target
}

object ProgressMilestones {
    private val streakTargets = listOf(3, 5, 7, 10, 14, 21)

    fun nextScoreGoal(score: Int, step: Int = 50): GoalProgress {
        val safeStep = step.coerceAtLeast(10)
        val safeScore = score.coerceAtLeast(0)
        return GoalProgress(current = safeScore % safeStep, target = safeStep)
    }

    fun nextStreakGoal(streak: Int): GoalProgress {
        val safeStreak = streak.coerceAtLeast(0)
        val target = streakTargets.firstOrNull { it > safeStreak } ?: (safeStreak + 7)
        return GoalProgress(current = safeStreak, target = target)
    }

    fun milestoneBadges(score: Int, streak: Int): List<String> {
        val badges = mutableListOf<String>()
        if (score >= 100) badges += "100 pikë"
        if (score >= 250) badges += "250 pikë"
        if (score >= 500) badges += "500 pikë"
        if (streak >= 3) badges += "Seri 3"
        if (streak >= 7) badges += "Seri 7"
        if (streak >= 14) badges += "Seri 14"
        return badges
    }

    fun dailyCompletionBadges(completedCount: Int, allComplete: Boolean): List<String> {
        val badges = mutableListOf<String>()
        if (completedCount >= 2) badges += "Dita aktive"
        if (allComplete) {
            val total = DailyChallengeKeys.all.size
            badges += "Sot $total/$total"
        }
        return badges
    }

    const val BADGE_SHKRIMTAR = "Shkrimtar"
    const val BADGE_RIMUES = "Rimues"
    const val BADGE_LEXUES = "Lexues"
}
