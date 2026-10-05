package com.albagram.dictionary.cache

import com.albagram.dictionary.model.DictionaryEntryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryCacheTest {
    private fun entry(term: String) = DictionaryEntryDto(
        term = term,
        definition = "def",
        sourceId = "src",
        sourceLabel = "Src",
        sourceUrl = "https://example.com/$term"
    )

    @Test
    fun putThenGet_returnsStoredResults() {
        val cache = DictionaryCache()
        cache.put("key", listOf(entry("shkolle")))
        assertEquals(1, cache.get("key")?.size)
    }

    @Test
    fun get_missingKey_returnsNull() {
        val cache = DictionaryCache()
        assertNull(cache.get("missing"))
    }

    @Test
    fun get_expiredEntry_returnsNullAndEvicts() {
        val cache = DictionaryCache(ttlMillis = -1)
        cache.put("key", listOf(entry("shkolle")))
        assertNull(cache.get("key"))
        assertNull(cache.get("key"))
    }

    @Test
    fun cacheKey_isNormalizedAndStable() {
        val cache = DictionaryCache()
        assertEquals(
            cache.cacheKey("src", "  Shkollë ", "exact"),
            cache.cacheKey("src", "shkollë", "exact")
        )
    }

    @Test
    fun put_beyondMaxEntries_evictsSomethingRatherThanGrowingUnbounded() {
        val cache = DictionaryCache(maxEntries = 3)
        repeat(10) { i -> cache.put("key$i", listOf(entry("w$i"))) }
        var stored = 0
        repeat(10) { i -> if (cache.get("key$i") != null) stored++ }
        assertTrue("expected cache to stay bounded, found $stored entries", stored <= 3)
    }

    @Test
    fun put_updatingExistingKey_doesNotCountAsNewEntryForEviction() {
        val cache = DictionaryCache(maxEntries = 2)
        cache.put("a", listOf(entry("a")))
        cache.put("b", listOf(entry("b")))
        cache.put("a", listOf(entry("a2")))
        assertEquals(1, cache.get("a")?.size)
        assertEquals(1, cache.get("b")?.size)
    }
}
