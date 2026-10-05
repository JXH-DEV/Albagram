package com.albagram.app.domain.ese

object EseCompletionRules {
    const val MIN_WORDS = 80

    private val whitespace = Regex("\\s+")

    fun wordCount(body: String): Int =
        body.trim().split(whitespace).count { it.isNotBlank() }

    fun canSubmit(body: String, suggestedTerms: List<String>): Boolean {
        if (wordCount(body) < MIN_WORDS) return false
        val terms = suggestedTerms.filter { it.isNotBlank() }
        if (terms.isEmpty()) return true
        return usedSuggestedCount(body, terms) > 0
    }

    fun usedSuggestedCount(body: String, suggestedTerms: List<String>): Int {
        val lower = body.lowercase()
        return suggestedTerms.count { term -> containsAtWordStart(lower, term.trim().lowercase()) }
    }

    private fun containsAtWordStart(lowerBody: String, term: String): Boolean {
        if (term.isEmpty()) return false
        var from = 0
        while (true) {
            val idx = lowerBody.indexOf(term, from)
            if (idx < 0) return false
            if (idx == 0 || !lowerBody[idx - 1].isLetterOrDigit()) return true
            from = idx + 1
        }
    }
}
