package com.albagram.app.ui.navigation

import android.util.Base64
import com.albagram.app.data.remote.DictionaryEntryDto
import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RemoteWordNav {
    fun encode(entry: RemoteDictionaryEntry, json: Json): String {
        val dto = DictionaryEntryDto(
            term = entry.term,
            definition = entry.definition,
            partOfSpeech = entry.partOfSpeech,
            sourceId = entry.sourceId,
            sourceLabel = entry.sourceLabel,
            sourceUrl = entry.sourceUrl
        )
        val bytes = json.encodeToString(dto).encodeToByteArray()
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decode(payload: String, json: Json): RemoteDictionaryEntry? {
        return runCatching {
            val text = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP).decodeToString()
            val dto = json.decodeFromString<DictionaryEntryDto>(text)
            RemoteDictionaryEntry(
                term = dto.term,
                definition = dto.definition,
                partOfSpeech = dto.partOfSpeech,
                sourceId = dto.sourceId,
                sourceLabel = dto.sourceLabel,
                sourceUrl = dto.sourceUrl
            )
        }.getOrNull()
    }
}
