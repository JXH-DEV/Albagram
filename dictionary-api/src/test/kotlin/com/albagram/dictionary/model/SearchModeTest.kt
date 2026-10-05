package com.albagram.dictionary.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchModeTest {
    @Test
    fun from_matchesByParamOrName_caseInsensitive() {
        assertEquals(SearchMode.PREFIX, SearchMode.from("prefix"))
        assertEquals(SearchMode.PREFIX, SearchMode.from("PREFIX"))
        assertEquals(SearchMode.CONTAINS, SearchMode.from("Contains"))
    }

    @Test
    fun from_nullOrBlankOrUnknown_defaultsToExact() {
        assertEquals(SearchMode.EXACT, SearchMode.from(null))
        assertEquals(SearchMode.EXACT, SearchMode.from(""))
        assertEquals(SearchMode.EXACT, SearchMode.from("nonsense"))
    }
}
