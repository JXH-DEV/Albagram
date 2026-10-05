package com.albagram.app.domain.rhyme

import com.albagram.app.data.local.entity.WordEntity
import kotlin.random.Random

data class RhymeQuizRound(
    val promptTerm: String,
    val options: List<String>,
    val correctIndex: Int
)

object RhymeQuizGenerator {
    fun buildRound(target: WordEntity, corpus: List<WordEntity>, random: Random = Random.Default): RhymeQuizRound? {
        if (corpus.size < 4) return null
        val rhymes = corpus.filter { word ->
            word.wordId != target.wordId &&
                RhymeMatcher.rhymeKeyFor(word.term) == RhymeMatcher.rhymeKeyFor(target.term) &&
                RhymeMatcher.normalize(word.term) != RhymeMatcher.normalize(target.term)
        }
        val correct = rhymes.randomOrNull(random) ?: return null
        val distractors = corpus
            .filter { it.wordId != target.wordId && it.wordId != correct.wordId }
            .shuffled(random)
            .take(3)
        if (distractors.size < 3) return null
        val options = (distractors.map { it.term } + correct.term).shuffled(random)
        val correctIndex = options.indexOf(correct.term)
        return RhymeQuizRound(
            promptTerm = target.term,
            options = options,
            correctIndex = correctIndex
        )
    }

    fun pickTargets(corpus: List<WordEntity>, count: Int = 5, random: Random = Random.Default): List<WordEntity> {
        return corpus.shuffled(random).take(count)
    }
}
