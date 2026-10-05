package com.albagram.dictionary.sources

import com.albagram.dictionary.cache.DictionaryCache
import com.albagram.dictionary.model.DictionaryEntryDto
import com.albagram.dictionary.model.SearchMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class DictionaryAggregatorTest {
    private class FakeSource(
        override val id: String,
        private val results: List<DictionaryEntryDto> = emptyList(),
        private val hangMillis: Long = 0,
        private val fail: Boolean = false
    ) : DictionarySource {
        override val label: String = id
        val calls = AtomicInteger()

        override suspend fun search(query: String, mode: SearchMode): List<DictionaryEntryDto> {
            calls.incrementAndGet()
            if (hangMillis > 0) delay(hangMillis)
            if (fail) error("boom")
            return results
        }
    }

    private fun entry(term: String, source: String) = DictionaryEntryDto(
        term = term,
        definition = "def of $term",
        sourceId = source,
        sourceLabel = source,
        sourceUrl = "https://example.com/$term"
    )

    private fun aggregator(vararg sources: DictionarySource) = DictionaryAggregator(
        sources = sources.toList(),
        cache = DictionaryCache(),
        sourceTimeoutMillis = 100,
        minRequestIntervalMillis = 0
    )

    @Test
    fun search_mergesSourcesAndDedupes() = runBlocking {
        val a = FakeSource("a", listOf(entry("shkollë", "a")))
        val b = FakeSource("b", listOf(entry("shkolle", "b"), entry("shkollar", "b")))
        val result = aggregator(a, b).search("shkollë", SearchMode.CONTAINS)
        assertFalse(result.partial)
        assertEquals(2, result.results.size)
    }

    @Test
    fun search_timedOutSource_isReportedAsPartial() = runBlocking {
        val ok = FakeSource("ok", listOf(entry("fjalë", "ok")))
        val slow = FakeSource("slow", hangMillis = 5_000)
        val result = aggregator(ok, slow).search("fjalë", SearchMode.EXACT)
        assertTrue(result.partial)
        assertEquals(listOf("slow"), result.failedSources)
        assertEquals(1, result.results.size)
    }

    @Test
    fun search_failingSource_doesNotFailTheWholeSearch() = runBlocking {
        val ok = FakeSource("ok", listOf(entry("fjalë", "ok")))
        val broken = FakeSource("broken", fail = true)
        val result = aggregator(ok, broken).search("fjalë", SearchMode.EXACT)
        assertEquals(1, result.results.size)
    }

    @Test
    fun search_partialResult_isNotServedFromCacheAsComplete() = runBlocking {
        val ok = FakeSource("ok", listOf(entry("fjalë", "ok")))
        val slow = FakeSource("slow", hangMillis = 5_000)
        val agg = aggregator(ok, slow)

        val first = agg.search("fjalë", SearchMode.EXACT)
        assertTrue(first.partial)

        val second = agg.search("fjalë", SearchMode.EXACT)
        assertTrue(second.partial)
    }

    @Test
    fun search_completeResult_isCached() = runBlocking {
        val a = FakeSource("a", listOf(entry("fjalë", "a")))
        val agg = aggregator(a)
        agg.search("fjalë", SearchMode.EXACT)
        agg.search("fjalë", SearchMode.EXACT)
        assertEquals(1, a.calls.get())
    }

    @Test
    fun lookup_usesCacheOnRepeat() = runBlocking {
        val a = FakeSource("a", listOf(entry("fjalë", "a")))
        val agg = aggregator(a)
        assertEquals("fjalë", agg.lookup("fjalë", null)?.term)
        agg.lookup("fjalë", null)
        assertEquals(1, a.calls.get())
    }
}
