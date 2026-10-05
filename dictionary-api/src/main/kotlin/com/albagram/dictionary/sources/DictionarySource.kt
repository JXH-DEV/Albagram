package com.albagram.dictionary.sources

import com.albagram.dictionary.model.DictionaryEntryDto
import com.albagram.dictionary.model.SearchMode

interface DictionarySource {
    val id: String
    val label: String

    suspend fun search(query: String, mode: SearchMode): List<DictionaryEntryDto>
}
