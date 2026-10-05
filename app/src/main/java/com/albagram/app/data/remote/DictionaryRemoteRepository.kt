package com.albagram.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRemoteRepository @Inject constructor(
    private val api: DictionaryApiService,
    @ApplicationContext private val context: Context
) {
    fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun search(query: String, mode: String = "contains"): Result<List<RemoteDictionaryEntry>> {
        if (!isOnline()) {
            return Result.failure(OfflineException())
        }
        return runCatching {
            withTimeout(SEARCH_TIMEOUT_MS) {
                retryOnIOException(attempts = 2) {
                    api.search(query.trim(), mode = mode).results.map { it.toDomain() }
                }
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    private suspend fun <T> retryOnIOException(attempts: Int, block: suspend () -> T): T {
        repeat(attempts - 1) {
            try {
                return block()
            } catch (e: IOException) {
                delay(300)
            }
        }
        return block()
    }

    private fun DictionaryEntryDto.toDomain() = RemoteDictionaryEntry(
        term = term,
        definition = definition,
        partOfSpeech = partOfSpeech,
        sourceId = sourceId,
        sourceLabel = sourceLabel,
        sourceUrl = sourceUrl
    )
}

private const val SEARCH_TIMEOUT_MS = 12_000L

class OfflineException : Exception("offline")
