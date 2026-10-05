package com.albagram.dictionary.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TermNormalizerTest {
    @Test
    fun normalize_stripsAlbanianDiacritics() {
        assertEquals("shkolle", TermNormalizer.normalize("shkollë"))
        assertEquals("cok", TermNormalizer.normalize("çok"))
    }

    @Test
    fun normalize_trimsAndLowercases() {
        assertEquals("fjale", TermNormalizer.normalize("  FJALË  "))
    }

    @Test
    fun normalize_isIdempotent() {
        val once = TermNormalizer.normalize("Shkollë e Çuditshme")
        assertEquals(once, TermNormalizer.normalize(once))
    }

    @Test
    fun normalize_treatsDiacriticVariantsAsEqual() {
        assertEquals(TermNormalizer.normalize("shkollë"), TermNormalizer.normalize("SHKOLLË"))
    }

    @Test
    fun firstLetter_returnsUppercaseFirstChar() {
        assertEquals("S", TermNormalizer.firstLetter("shkollë"))
        assertEquals("Ë", TermNormalizer.firstLetter("ëndje"))
    }

    @Test
    fun firstLetter_blankInput_returnsPlaceholder() {
        assertEquals("?", TermNormalizer.firstLetter("   "))
    }
}
