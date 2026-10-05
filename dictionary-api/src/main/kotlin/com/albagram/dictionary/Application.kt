package com.albagram.dictionary

import com.albagram.dictionary.cache.DictionaryCache
import com.albagram.dictionary.routes.dictionaryRoutes
import com.albagram.dictionary.sources.DictionaryAggregator
import com.albagram.dictionary.sources.FjaloriOnlineAdapter
import com.albagram.dictionary.sources.FjaloriShkencaAdapter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    install(ContentNegotiation) { json(json) }
    install(CallLogging) { level = Level.INFO }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal error"))
        }
    }

    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 12_000
        }
    }

    val cache = DictionaryCache()
    val sources = listOf(
        FjaloriOnlineAdapter(httpClient),
        FjaloriShkencaAdapter(httpClient)
    )
    val aggregator = DictionaryAggregator(sources, cache)

    routing {
        dictionaryRoutes(aggregator)
    }

    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        httpClient.close()
    }
}
