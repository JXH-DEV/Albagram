package com.albagram.app.domain.crossword

import com.albagram.app.data.seed.GridCellSeed
import com.albagram.app.data.seed.SeedClue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ClueDirection { ACROSS, DOWN }

data class CrosswordClue(
    val clueId: String,
    val wordId: String,
    val direction: ClueDirection,
    val number: Int,
    val row: Int,
    val col: Int,
    val length: Int,
    val clueText: String,
    val answer: String
)

object CrosswordGridBuilder {
    fun buildCells(width: Int, height: Int, clues: List<SeedClue>): List<GridCellSeed> {
        val letters = Array(height) { Array<Char?>(width) { null } }
        val numbers = Array(height) { Array<Int?>(width) { null } }
        val open = Array(height) { BooleanArray(width) { false } }

        for (clue in clues) {
            val answer = clue.answer.trim().uppercase()
            require(answer.length == clue.length) {
                "Clue ${clue.clueId} answer length mismatch"
            }
            numbers[clue.row][clue.col] = clue.number
            for (i in answer.indices) {
                val r = if (clue.direction.equals("DOWN", true)) clue.row + i else clue.row
                val c = if (clue.direction.equals("DOWN", true)) clue.col else clue.col + i
                require(r in 0 until height && c in 0 until width) {
                    "Clue ${clue.clueId} out of bounds at $r,$c"
                }
                val existing = letters[r][c]
                require(existing == null || existing == answer[i]) {
                    "Intersection conflict at $r,$c for ${clue.clueId}"
                }
                letters[r][c] = answer[i]
                open[r][c] = true
            }
        }

        val cells = mutableListOf<GridCellSeed>()
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (open[r][c]) {
                    cells += GridCellSeed(
                        row = r,
                        col = c,
                        block = false,
                        number = numbers[r][c],
                        letter = letters[r][c]?.toString()
                    )
                } else {
                    cells += GridCellSeed(row = r, col = c, block = true)
                }
            }
        }
        return cells
    }

    fun encodeGrid(width: Int, height: Int, clues: List<SeedClue>, json: Json): String {
        return json.encodeToString(buildCells(width, height, clues))
    }
}

object CrosswordEngine {
    fun cellsForClue(clue: CrosswordClue): List<Pair<Int, Int>> {
        return (0 until clue.length).map { i ->
            if (clue.direction == ClueDirection.DOWN) {
                clue.row + i to clue.col
            } else {
                clue.row to clue.col + i
            }
        }
    }

    fun readGuess(
        clue: CrosswordClue,
        entries: Map<Pair<Int, Int>, Char>
    ): String {
        return cellsForClue(clue).map { pos ->
            entries[pos] ?: ' '
        }.joinToString("")
    }

    fun isClueCorrect(
        clue: CrosswordClue,
        entries: Map<Pair<Int, Int>, Char>
    ): Boolean {
        val guess = readGuess(clue, entries).replace(" ", "")
        return guess.equals(clue.answer, ignoreCase = true)
    }

    fun nextEmptyInClue(
        clue: CrosswordClue,
        entries: Map<Pair<Int, Int>, Char>
    ): Pair<Int, Int>? {
        return cellsForClue(clue).firstOrNull { entries[it] == null }
    }
}
