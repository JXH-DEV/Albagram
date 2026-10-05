package com.albagram.dictionary.cache

import com.albagram.dictionary.model.DictionaryEntryDto
import java.util.concurrent.ConcurrentHashMap

class DictionaryCache(
    private val ttlMillis: Long = 24L * 60 * 60 * 1000,
    private val maxEntries: Int = 2_000
) {
    private data class CacheEntry(
        val results: List<DictionaryEntryDto>,
        val expiresAt: Long
    )

    private val store = ConcurrentHashMap<String, CacheEntry>()

    fun get(key: String): List<DictionaryEntryDto>? {
        val entry = store[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key)
            return null
        }
        return entry.results
    }

    fun put(key: String, results: List<DictionaryEntryDto>) {
        if (store.size >= maxEntries && !store.containsKey(key)) {
            store.keys.firstOrNull()?.let { store.remove(it) }
        }
        store[key] = CacheEntry(results, System.currentTimeMillis() + ttlMillis)
    }

    fun cacheKey(sourceId: String, query: String, mode: String): String =
        "$sourceId::$mode::${query.trim().lowercase()}"
}
