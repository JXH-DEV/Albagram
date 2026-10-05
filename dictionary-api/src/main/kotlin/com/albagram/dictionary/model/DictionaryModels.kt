package com.albagram.dictionary.model

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
data class DictionarySearchResponse(
    val query: String,
    val mode: String,
    val results: List<DictionaryEntryDto>,
    val partial: Boolean = false,
    val failedSources: List<String> = emptyList()
)

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val version: String = "1.5.0"
)

enum class SearchMode(val param: String) {
    EXACT("exact"),
    PREFIX("prefix"),
    CONTAINS("contains");

    companion object {
        fun from(value: String?): SearchMode = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) || it.param.equals(value, ignoreCase = true)
        } ?: EXACT
    }
}
