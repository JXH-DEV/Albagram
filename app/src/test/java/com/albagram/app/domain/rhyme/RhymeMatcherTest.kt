package com.albagram.app.domain.rhyme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhymeMatcherTest {
    @Test
    fun rhymeKey_usesTrailingLetters() {
        assertEquals("lla", RhymeMatcher.rhymeKeyFor("Shkolla"))
        assertEquals("olla", RhymeMatcher.rhymeKeyFor("Shkolla", preferredLength = 4))
    }

    @Test
    fun suffixesToTry_returnsDescending() {
        val suffixes = RhymeMatcher.suffixesToTry("Shkolla")
        assertTrue(suffixes.contains("olla"))
        assertTrue(suffixes.contains("lla"))
    }

    @Test
    fun normalize_mapsDiacriticsIncludingAccentedI() {
        assertEquals("rime", RhymeMatcher.normalize("Rimë"))
        assertEquals("rimi", RhymeMatcher.normalize("Rimí"))
        assertEquals("shkolla", RhymeMatcher.normalize("Shkolla"))
        assertEquals("ime", RhymeMatcher.rhymeKeyFor("Rimë"))
        assertEquals("imi", RhymeMatcher.rhymeKeyFor("Rimí"))
        assertEquals("lle", RhymeMatcher.rhymeKeyFor("mollë"))
        assertEquals(RhymeMatcher.normalize("Çaj"), RhymeMatcher.normalize("caj"))
    }
}
