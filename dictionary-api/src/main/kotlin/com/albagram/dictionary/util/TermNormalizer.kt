package com.albagram.dictionary.util

object TermNormalizer {
    fun normalize(input: String): String {
        return input.trim().lowercase()
            .replace('ë', 'e')
            .replace('ç', 'c')
            .replace('í', 'i')
            .replace('á', 'a')
            .replace('ó', 'o')
            .replace('ú', 'u')
    }

    fun firstLetter(term: String): String {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return "?"
        return trimmed.first().uppercaseChar().toString()
    }
}
