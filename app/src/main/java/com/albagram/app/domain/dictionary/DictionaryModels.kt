package com.albagram.app.domain.dictionary

data class RemoteDictionaryEntry(
    val term: String,
    val definition: String,
    val partOfSpeech: String? = null,
    val sourceId: String,
    val sourceLabel: String,
    val sourceUrl: String
)

data class RemoteSearchState(
    val loading: Boolean = false,
    val results: List<RemoteDictionaryEntry> = emptyList(),
    val error: String? = null,
    val offline: Boolean = false
)
