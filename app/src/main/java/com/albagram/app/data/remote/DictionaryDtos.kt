package com.albagram.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class DictionaryEntryDto(
    val term: String,
    val definition: String,
    val partOfSpeech: String? = null,
    val sourceId: String,
    val sourceLabel: String,
    val sourceUrl: String
)

@Serializable
data class DictionarySearchResponseDto(
    val query: String,
    val mode: String,
    val results: List<DictionaryEntryDto>,
    val partial: Boolean = false,
    val failedSources: List<String> = emptyList()
)
