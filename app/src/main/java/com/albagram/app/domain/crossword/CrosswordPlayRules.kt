package com.albagram.app.domain.crossword

object CrosswordPlayRules {
    fun shouldAwardScore(clueId: String, solvedClueIds: Set<String>): Boolean {
        return clueId !in solvedClueIds
    }

    fun scoreForClue(clueId: String, hintedClueIds: Set<String>): Int {
        return if (clueId in hintedClueIds) 0 else 10
    }

    fun isCellLocked(
        row: Int,
        col: Int,
        clues: List<CrosswordClue>,
        solvedClueIds: Set<String>
    ): Boolean {
        return clues.any { clue ->
            clue.clueId in solvedClueIds &&
                CrosswordEngine.cellsForClue(clue).any { it.first == row && it.second == col }
        }
    }

    fun resolveClueOnCellTap(
        covering: List<CrosswordClue>,
        current: CrosswordClue?
    ): CrosswordClue? {
        if (covering.isEmpty()) return null
        if (covering.size == 1) return covering.first()
        return when {
            current != null && covering.any { it.clueId == current.clueId } -> {
                covering.firstOrNull { it.clueId != current.clueId } ?: covering.first()
            }
            current != null -> {
                covering.firstOrNull { it.direction != current.direction } ?: covering.first()
            }
            else -> covering.first()
        }
    }
}
