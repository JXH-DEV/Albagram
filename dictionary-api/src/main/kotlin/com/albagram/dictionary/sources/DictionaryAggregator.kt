package com.albagram.dictionary.sources

import com.albagram.dictionary.cache.DictionaryCache
import com.albagram.dictionary.model.DictionaryEntryDto
import com.albagram.dictionary.model.SearchMode
import com.albagram.dictionary.util.TermNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DictionaryAggregator(
    private val sources: List<DictionarySource>,
    private val cache: DictionaryCache,
    private val sourceTimeoutMillis: Long = 8_000,
    private val minRequestIntervalMillis: Long = 1_000
) {
    private val lastRequestAt = ConcurrentHashMap<String, Long>()
    private val throttleLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun search(
        query: String,
        mode: SearchMode,
        limit: Int = 20
    ): SearchAggregateResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return SearchAggregateResult(emptyList(), partial = false, failedSources = emptyList())
        }

        val allCacheKey = cache.cacheKey("all", trimmed, mode.param)
        val cached = cache.get(allCacheKey)
        if (cached != null) {
            return SearchAggregateResult(cached.take(limit), partial = false, failedSources = emptyList())
        }

        val failedSources = CopyOnWriteArrayList<String>()
        val allResults = coroutineScope {
            sources.map { source ->
                async {
                    val cacheKey = cache.cacheKey(source.id, trimmed, mode.param)
                    cache.get(cacheKey)?.let { return@async it }

                    throttle(source.id)
                    val result = withTimeoutOrNull(sourceTimeoutMillis) {
                        runCatching { source.search(trimmed, mode) }
                            .onFailure { if (it is CancellationException) throw it }
                            .getOrElse { emptyList() }
                    } ?: run {
                        failedSources += source.id
                        emptyList()
                    }

                    if (result.isNotEmpty()) {
                        cache.put(cacheKey, result)
                    }
                    result
                }
            }.flatMap { it.await() }
        }

        val merged = mergeAndSort(allResults, trimmed, mode).take(limit)
        if (merged.isNotEmpty() && failedSources.isEmpty()) {
            cache.put(allCacheKey, merged)
        }

        return SearchAggregateResult(
            results = merged,
            partial = failedSources.isNotEmpty(),
            failedSources = failedSources.distinct()
        )
    }

    suspend fun lookup(term: String, sourceId: String?): DictionaryEntryDto? {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return null

        val candidates = if (sourceId.isNullOrBlank()) {
            sources
        } else {
            sources.filter { it.id == sourceId }
        }

        for (source in candidates) {
            val match = searchCached(source, trimmed, SearchMode.EXACT)
                ?.firstOrNull { TermNormalizer.normalize(it.term) == TermNormalizer.normalize(trimmed) }
                ?: searchCached(source, trimmed, SearchMode.CONTAINS)
                    ?.firstOrNull { TermNormalizer.normalize(it.term) == TermNormalizer.normalize(trimmed) }

            if (match != null) return match
        }
        return null
    }

    private suspend fun searchCached(
        source: DictionarySource,
        trimmed: String,
        mode: SearchMode
    ): List<DictionaryEntryDto>? {
        val cacheKey = cache.cacheKey(source.id, trimmed, mode.param)
        cache.get(cacheKey)?.let { return it }

        throttle(source.id)
        val result = withTimeoutOrNull(sourceTimeoutMillis) {
            runCatching { source.search(trimmed, mode) }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
        } ?: return null

        if (result.isNotEmpty()) {
            cache.put(cacheKey, result)
        }
        return result
    }

    internal fun mergeAndSort(
        entries: List<DictionaryEntryDto>,
        query: String,
        mode: SearchMode
    ): List<DictionaryEntryDto> {
        val normalizedQuery = TermNormalizer.normalize(query)
        val deduped = linkedMapOf<String, DictionaryEntryDto>()
        entries.forEach { entry ->
            val key = TermNormalizer.normalize(entry.term)
            if (key !in deduped) {
                deduped[key] = entry
            }
        }

        return deduped.values.sortedWith { a, b ->
            val rankA = matchRank(TermNormalizer.normalize(a.term), normalizedQuery, mode)
            val rankB = matchRank(TermNormalizer.normalize(b.term), normalizedQuery, mode)
            if (rankA != rankB) rankA - rankB else a.term.compareTo(b.term, ignoreCase = true)
        }
    }

    private fun matchRank(term: String, query: String, mode: SearchMode): Int {
        return when {
            term == query -> 0
            term.startsWith(query) -> 1
            mode == SearchMode.CONTAINS && term.contains(query) -> 2
            else -> 3
        }
    }

    private suspend fun throttle(sourceId: String) {
        val mutex = throttleLocks.computeIfAbsent(sourceId) { Mutex() }
        mutex.withLock {
            val now = System.currentTimeMillis()
            val last = lastRequestAt[sourceId] ?: 0L
            val wait = minRequestIntervalMillis - (now - last)
            if (wait > 0) {
                kotlinx.coroutines.delay(wait)
            }
            lastRequestAt[sourceId] = System.currentTimeMillis()
        }
    }
}

data class SearchAggregateResult(
    val results: List<DictionaryEntryDto>,
    val partial: Boolean,
    val failedSources: List<String>
)
