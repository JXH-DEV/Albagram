package com.albagram.app.domain.ese

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EseCompletionRulesTest {
    @Test
    fun canSubmit_requiresWordCountAndSuggestedWord() {
        val body = (1..80).joinToString(" ") { "fjale$it" } + " argument"
        assertTrue(EseCompletionRules.canSubmit(body, listOf("argument", "strofë")))
    }

    @Test
    fun canSubmit_rejectsShortBody() {
        assertFalse(EseCompletionRules.canSubmit("shkurt", listOf("argument")))
    }

    @Test
    fun canSubmit_allowsWhenNoSuggestedWords() {
        val body = (1..80).joinToString(" ") { "fjale$it" }
        assertTrue(EseCompletionRules.canSubmit(body, emptyList()))
    }

    @Test
    fun usedSuggestedCount_acceptsInflectedForms() {
        assertEquals(1, EseCompletionRules.usedSuggestedCount("Ai e do punën e tij", listOf("punë")))
    }

    @Test
    fun usedSuggestedCount_rejectsTermBuriedInsideAnotherWord() {
        assertEquals(0, EseCompletionRules.usedSuggestedCount("papunësia është problem", listOf("punë")))
    }

    @Test
    fun usedSuggestedCount_isCaseInsensitiveAndHandlesDiacritics() {
        assertEquals(1, EseCompletionRules.usedSuggestedCount("Shkollë e mirë", listOf("SHKOLLË")))
    }

    @Test
    fun usedSuggestedCount_findsLaterOccurrenceAfterEmbeddedOne() {
        assertEquals(1, EseCompletionRules.usedSuggestedCount("papunësia dhe punë", listOf("punë")))
    }

    @Test
    fun usedSuggestedCount_ignoresBlankTerms() {
        assertEquals(0, EseCompletionRules.usedSuggestedCount("çfarëdo tekst", listOf("", "  ")))
    }

    @Test
    fun canSubmit_withOnlyBlankSuggestedTerms_isNotBlocked() {
        val body = (1..80).joinToString(" ") { "fjale$it" }
        assertTrue(EseCompletionRules.canSubmit(body, listOf("")))
    }

    @Test
    fun canSubmit_rejectsWhenOnlyEmbeddedMatchExists() {
        val body = (1..80).joinToString(" ") { "fjale$it" } + " papunësia"
        assertFalse(EseCompletionRules.canSubmit(body, listOf("punë")))
    }
}
