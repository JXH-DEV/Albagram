package com.albagram.app.ui.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Crossword : Routes("crossword")
    data object CrosswordPlay : Routes("crossword/{puzzleId}") {
        fun create(puzzleId: String) = "crossword/$puzzleId"
    }
    data object Rima : Routes("rima")
    data object Ese : Routes("ese")
    data object EseWrite : Routes("ese/{promptId}") {
        fun create(promptId: String) = "ese/$promptId"
    }
    data object Konkursi : Routes("konkursi?daily={daily}") {
        fun create(daily: Boolean = false) = "konkursi?daily=$daily"
    }
    data object Libri : Routes("libri")
    data object Glossary : Routes("glossary")
    data object WordDetail : Routes("word/{wordId}") {
        fun create(wordId: String) = "word/$wordId"
    }

    data object RemoteWordDetail : Routes("remote_word/{payload}") {
        fun create(payload: String) = "remote_word/$payload"
    }
}

enum class BottomDest(
    val route: String,
    val navRoute: String,
    val label: String
) {
    Crossword(Routes.Crossword.route, Routes.Crossword.route, "Fjalëkryqi"),
    Rima(Routes.Rima.route, Routes.Rima.route, "Rima"),
    Ese(Routes.Ese.route, Routes.Ese.route, "Ese"),
    Konkursi(Routes.Konkursi.route, Routes.Konkursi.create(), "Konkursi"),
    Libri(Routes.Libri.route, Routes.Libri.route, "Libri im")
}
