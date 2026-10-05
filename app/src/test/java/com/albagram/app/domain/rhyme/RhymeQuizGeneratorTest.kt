package com.albagram.app.domain.rhyme

import com.albagram.app.data.local.entity.WordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RhymeQuizGeneratorTest {
    private fun word(id: String, term: String, key: String) = WordEntity(
        wordId = id,
        term = term,
        definition = "def $term",
        letterOfAlphabet = "A",
        rhymeKey = key,
        inGlossary = true
    )

    @Test
    fun buildRound_hasOneCorrectAmongFour() {
        val corpus = listOf(
            word("1", "shkolla", "olla"),
            word("2", "rruga", "uga"),
            word("3", "molla", "olla"),
            word("4", "bolla", "olla"),
            word("5", "kulla", "ulla")
        )
        val round = RhymeQuizGenerator.buildRound(corpus[0], corpus, Random(42))
        requireNotNull(round)
        assertEquals(4, round.options.size)
        assertTrue(round.correctIndex in round.options.indices)
        val correct = round.options[round.correctIndex]
        assertTrue(correct != round.promptTerm)
        assertTrue(corpus.any { it.term == correct && it.wordId != corpus[0].wordId })
    }
}
