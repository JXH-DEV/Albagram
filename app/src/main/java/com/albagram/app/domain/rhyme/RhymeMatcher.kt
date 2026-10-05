package com.albagram.app.domain.rhyme

object RhymeMatcher {
    fun normalize(input: String): String {
        return input.trim().lowercase()
            .replace('ë', 'e')
            .replace('Ë', 'e')
            .replace('ç', 'c')
            .replace('Ç', 'c')
            .replace('í', 'i')
            .replace('Í', 'i')
            .replace('á', 'a')
            .replace('Á', 'a')
            .replace('ó', 'o')
            .replace('Ó', 'o')
            .replace('ú', 'u')
            .replace('Ú', 'u')
    }

    fun rhymeKeyFor(term: String, preferredLength: Int = 3): String {
        val n = normalize(term).filter { it.isLetter() }
        if (n.isEmpty()) return ""
        return if (n.length <= preferredLength) n else n.takeLast(preferredLength)
    }

    fun suffixesToTry(term: String): List<String> {
        val n = normalize(term).filter { it.isLetter() }
        if (n.length < 2) return emptyList()
        val lengths = listOf(4, 3, 2).filter { it <= n.length }
        return lengths.map { n.takeLast(it) }.distinct()
    }
}
