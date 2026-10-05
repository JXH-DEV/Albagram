package com.albagram.dictionary.routes

import com.albagram.dictionary.model.DictionarySearchResponse
import com.albagram.dictionary.model.HealthResponse
import com.albagram.dictionary.model.SearchMode
import com.albagram.dictionary.sources.DictionaryAggregator
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private const val MAX_QUERY_LENGTH = 150

fun Route.dictionaryRoutes(aggregator: DictionaryAggregator) {
    get("/health") {
        call.respond(HealthResponse())
    }

    get("/v1/dictionary/search") {
        val query = call.request.queryParameters["q"].orEmpty()
        if (query.isBlank() || query.length > MAX_QUERY_LENGTH) {
            call.respond(
                HttpStatusCode.BadRequest,
                DictionarySearchResponse(query = query, mode = SearchMode.EXACT.param, results = emptyList())
            )
            return@get
        }

        val mode = SearchMode.from(call.request.queryParameters["mode"])
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
        val result = aggregator.search(query, mode, limit)

        call.respond(
            DictionarySearchResponse(
                query = query,
                mode = mode.param,
                results = result.results,
                partial = result.partial,
                failedSources = result.failedSources
            )
        )
    }

    get("/v1/dictionary/lookup") {
        val term = call.request.queryParameters["term"].orEmpty()
        if (term.isBlank() || term.length > MAX_QUERY_LENGTH) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "term is required"))
            return@get
        }

        val sourceId = call.request.queryParameters["source"]
        val entry = aggregator.lookup(term, sourceId)
        if (entry == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "entry not found"))
        } else {
            call.respond(entry)
        }
    }
}
