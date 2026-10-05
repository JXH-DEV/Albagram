package com.albagram.dictionary.sources

import com.albagram.dictionary.model.DictionaryEntryDto
import com.albagram.dictionary.model.SearchMode
import com.albagram.dictionary.util.TermNormalizer
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FjaloriShkencaAdapter(
    private val client: HttpClient,
    private val baseUrl: String = "http://www.fjalori.shkenca.org/"
) : DictionarySource {
    override val id: String = "fjalori.shkenca.org"
    override val label: String = "Fjalori shpjegues (shkenca.org)"

    override suspend fun search(query: String, mode: SearchMode): List<DictionaryEntryDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val html = client.submitForm(
            url = "${baseUrl}text.php",
            formParameters = Parameters.build {
                append("eingabe", trimmed)
            }
        ) {
            header("User-Agent", FjaloriOnlineAdapter.USER_AGENT)
        }.bodyAsText()

        val entries = parseHtml(html)
        return filterByMode(entries, trimmed, mode)
    }

    internal fun parseHtml(html: String): List<DictionaryEntryDto> {
        if (html.isBlank()) return emptyList()
        val doc = Jsoup.parseBodyFragment(html)
        val entries = mutableListOf<DictionaryEntryDto>()

        doc.select("dt").forEach { dt ->
            val termElement = dt.selectFirst("b") ?: return@forEach
            val term = termElement.text().trim()
            if (term.isBlank()) return@forEach

            val pos = dt.selectFirst("i")?.text()?.trim()?.removeSuffix(".")
            val dd = dt.nextElementSibling()
            val definition = if (dd != null && dd.tagName() == "dd") {
                dd.text().trim()
            } else {
                ""
            }
            if (definition.isBlank()) return@forEach

            entries += DictionaryEntryDto(
                term = term,
                definition = definition,
                partOfSpeech = pos?.takeIf { it.isNotBlank() },
                sourceId = id,
                sourceLabel = label,
                sourceUrl = buildEntryUrl(term)
            )
        }

        return entries.distinctBy { TermNormalizer.normalize(it.term) }
    }

    private fun filterByMode(
        entries: List<DictionaryEntryDto>,
        query: String,
        mode: SearchMode
    ): List<DictionaryEntryDto> {
        val normalizedQuery = TermNormalizer.normalize(query)
        return when (mode) {
            SearchMode.EXACT -> entries.filter { TermNormalizer.normalize(it.term) == normalizedQuery }
            SearchMode.PREFIX -> entries.filter {
                TermNormalizer.normalize(it.term).startsWith(normalizedQuery)
            }
            SearchMode.CONTAINS -> entries.filter {
                TermNormalizer.normalize(it.term).contains(normalizedQuery) ||
                    TermNormalizer.normalize(it.definition).contains(normalizedQuery)
            }
        }
    }

    private fun buildEntryUrl(term: String): String {
        val encoded = URLEncoder.encode(term, StandardCharsets.UTF_8)
        return "${baseUrl}text.php?eingabe=$encoded"
    }
}
