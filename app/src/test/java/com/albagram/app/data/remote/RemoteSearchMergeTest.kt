package com.albagram.app.data.remote

import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import com.albagram.app.domain.rhyme.RhymeMatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteSearchMergeTest {
    @Test
    fun remoteResultsExcludeLocalDuplicates() {
        val localTerms = listOf("Shkollë", "Libër").map { RhymeMatcher.normalize(it) }.toSet()
        val remote = listOf(
            entry("SHKOLLE", "online def"),
            entry("UNIVERSITET", "campus")
        )
        val filtered = remote.filter { RhymeMatcher.normalize(it.term) !in localTerms }
        assertEquals(1, filtered.size)
        assertEquals("UNIVERSITET", filtered.first().term)
    }

    private fun entry(term: String, definition: String) = RemoteDictionaryEntry(
        term = term,
        definition = definition,
        sourceId = "fjalori.online",
        sourceLabel = "Test",
        sourceUrl = "https://example.com"
    )
}
