package com.albagram.dictionary.sources

import com.albagram.dictionary.model.DictionaryEntryDto
import com.albagram.dictionary.model.SearchMode
import com.albagram.dictionary.util.TermNormalizer
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FjaloriOnlineAdapter(
    private val client: HttpClient,
    private val baseUrl: String = "https://fjalori.online/"
) : DictionarySource {
    override val id: String = "fjalori.online"
    override val label: String = "Fjalor i Madh (fjalori.online)"

    override suspend fun search(query: String, mode: SearchMode): List<DictionaryEntryDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8)
        val modeParam = when (mode) {
            SearchMode.EXACT -> "exact"
            SearchMode.PREFIX -> "prefix"
            SearchMode.CONTAINS -> "contains"
        }
        val url = "${baseUrl}?mode=$modeParam&search=$encoded"
        val html = client.get(url) {
            header("User-Agent", USER_AGENT)
        }.bodyAsText()

        return parseHtml(html, url)
    }

    internal fun parseHtml(html: String, sourceUrl: String): List<DictionaryEntryDto> {
        val doc = Jsoup.parse(html)
        val entries = mutableListOf<DictionaryEntryDto>()

        doc.select("article.dictionary-entry").forEach { article ->
            parseEntryArticle(article, sourceUrl)?.let(entries::add)
        }

        if (entries.isEmpty()) {
            doc.select("#rezultatet .similar-result, #rezultatet .list-group-item").forEach { block ->
                parseSimilarBlock(block.text(), sourceUrl)?.let(entries::add)
            }
        }

        return entries.distinctBy { TermNormalizer.normalize(it.term) }
    }

    private fun parseEntryArticle(
        article: org.jsoup.nodes.Element,
        sourceUrl: String
    ): DictionaryEntryDto? {
        val term = article.attr("data-word")
            .ifBlank { article.selectFirst(".dictionary-entry-head")?.text()?.trim().orEmpty() }
        if (term.isBlank()) return null

        val body = article.selectFirst(".dictionary-entry-body")
        val definition = body?.text()?.trim().orEmpty()
        if (definition.isBlank()) return null

        val partOfSpeech = body?.selectFirst(".abbr-tip[title*=gjinis]")?.text()?.trim()
            ?: body?.selectFirst("i .entry-meta-salmon")?.text()?.trim()

        val entryUrl = buildEntryUrl(term)
        return DictionaryEntryDto(
            term = term,
            definition = definition,
            partOfSpeech = partOfSpeech?.takeIf { it.isNotBlank() },
            sourceId = id,
            sourceLabel = label,
            sourceUrl = entryUrl
        )
    }

    private fun parseSimilarBlock(text: String, sourceUrl: String): DictionaryEntryDto? {
        val cleaned = text.trim()
        if (cleaned.length < 4) return null
        val term = cleaned.takeWhile { it.isLetter() }
            .ifBlank { return null }
        return DictionaryEntryDto(
            term = term,
            definition = cleaned,
            partOfSpeech = null,
            sourceId = id,
            sourceLabel = label,
            sourceUrl = buildEntryUrl(term)
        )
    }

    private fun buildEntryUrl(term: String): String {
        val encoded = URLEncoder.encode(term, StandardCharsets.UTF_8)
        return "${baseUrl}?mode=exact&search=$encoded"
    }

    companion object {
        const val USER_AGENT = "AlbagramDictionaryBot/1.5 (+educational; local-dev)"
    }
}
