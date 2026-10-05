package com.albagram.dictionary.sources

import com.albagram.dictionary.model.DictionaryEntryDto
import com.albagram.dictionary.model.SearchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryParserTest {
    private val onlineAdapter = FjaloriOnlineAdapter(io.ktor.client.HttpClient())
    private val shkencaAdapter = FjaloriShkencaAdapter(io.ktor.client.HttpClient())

    @Test
    fun fjaloriOnline_parsesExactEntry() {
        val html = readFixture("fixtures/fjalori_online_shkolle.html")
        val entries = onlineAdapter.parseHtml(html, "https://fjalori.online/?mode=exact&search=shkoll%C3%AB")
        assertEquals(1, entries.size)
        assertEquals("SHKOLLE", entries.first().term)
        assertTrue(entries.first().definition.contains("Institucion arsimor"))
        assertEquals("fjalori.online", entries.first().sourceId)
    }

    @Test
    fun shkenca_parsesEntries() {
        val html = readFixture("fixtures/shkenca_shkolle.html")
        val entries = shkencaAdapter.parseHtml(html)
        assertEquals(2, entries.size)
        assertEquals("SHKOLLE", entries.first().term)
        assertEquals("f", entries.first().partOfSpeech)
    }

    @Test
    fun aggregator_dedupesAndSortsExactFirst() {
        val aggregator = DictionaryAggregator(emptyList(), com.albagram.dictionary.cache.DictionaryCache())
        val entries = listOf(
            entry("SHKOLLAR", "def b"),
            entry("SHKOLLE", "def a"),
            entry("shkollë", "def duplicate")
        )
        val sorted = aggregator.mergeAndSort(entries, "shkollë", SearchMode.EXACT)
        assertEquals(2, sorted.size)
        assertEquals("SHKOLLE", sorted.first().term)
    }

    private fun entry(term: String, definition: String) = DictionaryEntryDto(
        term = term,
        definition = definition,
        sourceId = "test",
        sourceLabel = "Test",
        sourceUrl = "https://example.com"
    )

    private fun readFixture(path: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(path)) {
            "Missing fixture: $path"
        }
        return stream.bufferedReader().use { it.readText() }
    }
}
