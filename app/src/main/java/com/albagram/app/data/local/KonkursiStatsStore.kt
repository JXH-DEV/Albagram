package com.albagram.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class KonkursiStats(
    val bestScore: Int = 0,
    val lastScore: Int = 0,
    val lastCorrect: Int = 0,
    val lastWrong: Int = 0,
    val lastTimeout: Int = 0
)

@Singleton
class KonkursiStatsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun load(): KonkursiStats = withContext(Dispatchers.IO) {
        KonkursiStats(
            bestScore = prefs.getInt(KEY_BEST, 0),
            lastScore = prefs.getInt(KEY_LAST_SCORE, 0),
            lastCorrect = prefs.getInt(KEY_LAST_CORRECT, 0),
            lastWrong = prefs.getInt(KEY_LAST_WRONG, 0),
            lastTimeout = prefs.getInt(KEY_LAST_TIMEOUT, 0)
        )
    }

    suspend fun recordSession(score: Int, correct: Int, wrong: Int, timeout: Int) = withContext(Dispatchers.IO) {
        val best = maxOf(prefs.getInt(KEY_BEST, 0), score)
        prefs.edit()
            .putInt(KEY_BEST, best)
            .putInt(KEY_LAST_SCORE, score)
            .putInt(KEY_LAST_CORRECT, correct)
            .putInt(KEY_LAST_WRONG, wrong)
            .putInt(KEY_LAST_TIMEOUT, timeout)
            .apply()
    }

    companion object {
        private const val PREFS = "albagram_konkursi_stats"
        private const val KEY_BEST = "best_score"
        private const val KEY_LAST_SCORE = "last_score"
        private const val KEY_LAST_CORRECT = "last_correct"
        private const val KEY_LAST_WRONG = "last_wrong"
        private const val KEY_LAST_TIMEOUT = "last_timeout"
    }
}
