package com.albagram.app.data.repository

import com.albagram.app.data.local.dao.CrosswordDao
import com.albagram.app.data.local.dao.EssayDao
import com.albagram.app.data.local.dao.QuizDao
import com.albagram.app.data.local.dao.WordDao
import com.albagram.app.domain.daily.DailyChallengeEngine
import com.albagram.app.domain.daily.DailyChallengeSet
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyChallengeRepository @Inject constructor(
    private val wordDao: WordDao,
    private val crosswordDao: CrosswordDao,
    private val quizDao: QuizDao,
    private val essayDao: EssayDao
) {
    suspend fun forDate(date: LocalDate = LocalDate.now()): DailyChallengeSet {
        val wordIds = wordDao.getGlossaryWordIds()
        val puzzleIds = crosswordDao.getAllPuzzleIds()
        val quizIds = quizDao.getAllIds()
        val essayIds = essayDao.getAllPromptIds()
        return DailyChallengeEngine.build(date, wordIds, puzzleIds, quizIds, essayIds)
    }
}
