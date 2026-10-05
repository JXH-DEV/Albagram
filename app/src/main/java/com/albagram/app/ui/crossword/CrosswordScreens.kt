package com.albagram.app.ui.crossword

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.local.entity.CrosswordPuzzleEntity
import com.albagram.app.data.repository.BookRepository
import com.albagram.app.data.repository.CrosswordRepository
import com.albagram.app.data.repository.DailyChallengeRepository
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.domain.daily.DailyChallengeKeys
import com.albagram.app.data.seed.GridCellSeed
import com.albagram.app.domain.crossword.ClueDirection
import com.albagram.app.domain.crossword.CrosswordClue
import com.albagram.app.domain.crossword.CrosswordEngine
import com.albagram.app.domain.crossword.CrosswordPlayRules
import com.albagram.app.ui.components.AlbagramHaptics
import com.albagram.app.ui.components.AlbagramScaffold
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.LoadingState
import com.albagram.app.ui.components.ModuleCard
import com.albagram.app.ui.components.ModuleContentState
import com.albagram.app.ui.components.ModuleScreenHeader
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SessionSummaryCard
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.theme.AlbagramDimens
import com.albagram.app.ui.theme.Coral
import com.albagram.app.ui.theme.Forest
import com.albagram.app.ui.theme.Gold
import com.albagram.app.ui.theme.Leaf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class CrosswordListViewModel @Inject constructor(
    repo: CrosswordRepository
) : ViewModel() {
    private val loaded = MutableStateFlow(false)
    val puzzles = repo.observePuzzles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val progress = repo.observeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            puzzles.collect {
                loaded.value = true
            }
        }
    }

    val isLoaded = loaded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

@Composable
fun CrosswordListScreen(
    contentPadding: PaddingValues,
    onOpenPuzzle: (String) -> Unit,
    onHome: (() -> Unit)? = null,
    vm: CrosswordListViewModel = hiltViewModel()
) {
    val puzzles by vm.puzzles.collectAsStateWithLifecycle()
    val loaded by vm.isLoaded.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val progressById = remember(progress) { progress.associateBy { it.puzzleId } }
    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(AlbagramDimens.screenPadding)
        ) {
            ModuleScreenHeader(
                title = stringResource(R.string.crossword_title),
                subtitle = stringResource(R.string.crossword_subtitle),
                onHome = onHome
            )
            Spacer(Modifier.height(12.dp))
            ModuleContentState(
                loading = !loaded,
                empty = puzzles.isEmpty(),
                emptyText = stringResource(R.string.crossword_empty)
            ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AlbagramDimens.itemGap)
                    ) {
                        items(puzzles, key = { it.puzzleId }) { puzzle ->
                            val status = puzzleStatusLabel(progressById[puzzle.puzzleId])
                            ModuleCard(
                                title = puzzle.title,
                                subtitle = "${puzzle.width}×${puzzle.height} · $status",
                                icon = Icons.Default.Extension,
                                onClick = { onOpenPuzzle(puzzle.puzzleId) }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun puzzleStatusLabel(progress: com.albagram.app.data.local.entity.CrosswordProgressEntity?): String {
    if (progress == null) return stringResource(R.string.crossword_status_new)
    return when {
        progress.completed -> stringResource(R.string.crossword_status_done)
        progress.entriesJson != "{}" || progress.solvedClueIdsJson != "[]" ->
            stringResource(R.string.crossword_status_progress)
        else -> stringResource(R.string.crossword_status_new)
    }
}

data class CrosswordUiState(
    val puzzle: CrosswordPuzzleEntity? = null,
    val cells: List<GridCellSeed> = emptyList(),
    val clues: List<CrosswordClue> = emptyList(),
    val selectedClueId: String? = null,
    val selectedCell: Pair<Int, Int>? = null,
    val loadFailed: Boolean = false,
    val loading: Boolean = true,
    val feedback: String? = null,
    val feedbackPositive: Boolean = false,
    val checking: Boolean = false,
    val solvedClueIds: Set<String> = emptySet(),
    val hintedClueIds: Set<String> = emptySet(),
    val revealDefinitionFor: String? = null,
    val definitionText: String? = null,
    val suggestSaveWordId: String? = null,
    val sessionPoints: Int = 0,
    val sessionHints: Int = 0,
    val showCompletionOverlay: Boolean = false
)

@HiltViewModel
class CrosswordPlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val crosswordRepository: CrosswordRepository,
    private val wordRepository: WordRepository,
    private val bookRepository: BookRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val json: Json
) : ViewModel() {
    private val puzzleId: String = checkNotNull(savedStateHandle["puzzleId"])

    var ui by mutableStateOf(CrosswordUiState())
        private set

    val entries = mutableStateMapOf<Pair<Int, Int>, Char>()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun persist() {
        crosswordRepository.saveProgress(
            puzzleId = puzzleId,
            entries = entries.toMap(),
            solvedClueIds = ui.solvedClueIds,
            hintedClueIds = ui.hintedClueIds,
            completed = ui.clues.isNotEmpty() && ui.solvedClueIds.size >= ui.clues.size
        )
    }

    private suspend fun load() {
        try {
            val puzzle = crosswordRepository.getPuzzle(puzzleId)
            if (puzzle == null) {
                ui = ui.copy(loading = false, loadFailed = true)
                return
            }
            val cells = json.decodeFromString<List<GridCellSeed>>(puzzle.gridJson)
            val clues = crosswordRepository.getClues(puzzleId)
            val saved = crosswordRepository.getProgress(puzzleId)
            if (saved != null) {
                entries.clear()
                entries.putAll(crosswordRepository.decodeEntries(saved))
            }
            val solved = saved?.let { crosswordRepository.decodeIdSet(it.solvedClueIdsJson) }.orEmpty()
            val hinted = saved?.let { crosswordRepository.decodeIdSet(it.hintedClueIdsJson) }.orEmpty()
            val first = clues.firstOrNull { it.clueId !in solved } ?: clues.firstOrNull()
            ui = ui.copy(
                puzzle = puzzle,
                cells = cells,
                clues = clues,
                selectedClueId = first?.clueId,
                selectedCell = first?.let { it.row to it.col },
                solvedClueIds = solved,
                hintedClueIds = hinted,
                loading = false,
                loadFailed = false,
                sessionPoints = 0,
                sessionHints = 0
            )
        } catch (_: Exception) {
            ui = ui.copy(loading = false, loadFailed = true)
        }
    }

    fun retryLoad() {
        viewModelScope.launch {
            ui = ui.copy(loading = true, loadFailed = false, feedback = null)
            load()
        }
    }

    fun selectClue(clueId: String) {
        val clue = ui.clues.firstOrNull { it.clueId == clueId } ?: return
        val start = CrosswordEngine.nextEmptyInClue(clue, entries.toMap())
            ?: CrosswordEngine.cellsForClue(clue).first()
        ui = ui.copy(
            selectedClueId = clueId,
            selectedCell = start,
            feedback = null,
            revealDefinitionFor = null,
            definitionText = null,
            suggestSaveWordId = null
        )
    }

    fun selectCell(row: Int, col: Int) {
        val covering = ui.clues.filter { c ->
            CrosswordEngine.cellsForClue(c).any { it.first == row && it.second == col }
        }
        val nextClue = CrosswordPlayRules.resolveClueOnCellTap(covering, selectedClue()) ?: return
        ui = ui.copy(
            selectedClueId = nextClue.clueId,
            selectedCell = row to col,
            feedback = null,
            revealDefinitionFor = null,
            definitionText = null,
            suggestSaveWordId = null
        )
    }

    fun typeLetter(char: Char) {
        val clue = selectedClue() ?: return
        if (clue.clueId in ui.solvedClueIds) return
        if (!char.isLetter()) return
        val cells = CrosswordEngine.cellsForClue(clue)
        var target = ui.selectedCell?.takeIf { it in cells }
            ?: CrosswordEngine.nextEmptyInClue(clue, entries.toMap())
            ?: cells.last()
        if (CrosswordPlayRules.isCellLocked(target.first, target.second, ui.clues, ui.solvedClueIds)) {
            target = cells.firstOrNull {
                !CrosswordPlayRules.isCellLocked(it.first, it.second, ui.clues, ui.solvedClueIds) &&
                    entries[it] == null
            } ?: return
        }
        entries[target] = char.uppercaseChar()
        val idx = cells.indexOf(target)
        val next = cells.drop(idx + 1).firstOrNull {
            !CrosswordPlayRules.isCellLocked(it.first, it.second, ui.clues, ui.solvedClueIds)
        }
        ui = ui.copy(selectedCell = next ?: target)
        viewModelScope.launch { persist() }
    }

    fun clearSelectedCell() {
        val clue = selectedClue() ?: return
        if (clue.clueId in ui.solvedClueIds) return
        val cells = CrosswordEngine.cellsForClue(clue)
        val current = ui.selectedCell?.takeIf { it in cells }
        when {
            current != null && entries[current] != null &&
                !CrosswordPlayRules.isCellLocked(current.first, current.second, ui.clues, ui.solvedClueIds) -> {
                entries.remove(current)
            }
            current != null -> {
                val idx = cells.indexOf(current)
                val prev = cells.take(idx).lastOrNull {
                    !CrosswordPlayRules.isCellLocked(it.first, it.second, ui.clues, ui.solvedClueIds)
                } ?: return
                entries.remove(prev)
                ui = ui.copy(selectedCell = prev)
            }
            else -> {
                val filled = cells.lastOrNull {
                    entries[it] != null &&
                        !CrosswordPlayRules.isCellLocked(it.first, it.second, ui.clues, ui.solvedClueIds)
                } ?: return
                entries.remove(filled)
                ui = ui.copy(selectedCell = filled)
            }
        }
        viewModelScope.launch { persist() }
    }

    fun selectedClue(): CrosswordClue? = ui.clues.firstOrNull { it.clueId == ui.selectedClueId }

    fun checkAnswer() {
        val clue = selectedClue() ?: return
        if (!CrosswordPlayRules.shouldAwardScore(clue.clueId, ui.solvedClueIds)) {
            ui = ui.copy(feedback = "Kjo fjalë është zgjidhur tashmë.", feedbackPositive = true)
            return
        }
        if (ui.checking) return
        viewModelScope.launch {
            ui = ui.copy(checking = true, feedback = null)
            delay(700)
            val correct = CrosswordEngine.isClueCorrect(clue, entries.toMap())
            if (correct) {
                val word = wordRepository.getById(clue.wordId)
                val solved = ui.solvedClueIds + clue.clueId
                val points = CrosswordPlayRules.scoreForClue(clue.clueId, ui.hintedClueIds)
                val complete = solved.size == ui.clues.size
                ui = ui.copy(
                    checking = false,
                    feedback = if (points > 0) "Përgjigjja e saktë!" else "E saktë (me ndihmë) — pa pikë.",
                    feedbackPositive = true,
                    solvedClueIds = solved,
                    revealDefinitionFor = clue.wordId,
                    definitionText = word?.definition,
                    suggestSaveWordId = clue.wordId,
                    sessionPoints = ui.sessionPoints + points,
                    showCompletionOverlay = complete
                )
                if (points > 0) progressRepository.addScore(points)
                if (complete) {
                    progressRepository.awardBadge("Mjeshtri i fjalëkryqit")
                    val daily = dailyChallengeRepository.forDate()
                    if (daily.puzzleId == puzzleId) {
                        progressRepository.markDailyComplete(DailyChallengeKeys.CROSSWORD)
                    }
                }
                persist()
            } else {
                ui = ui.copy(
                    checking = false,
                    feedback = "Gabim",
                    feedbackPositive = false,
                    revealDefinitionFor = null,
                    definitionText = null,
                    suggestSaveWordId = clue.wordId
                )
                CrosswordEngine.cellsForClue(clue).forEach { pos ->
                    if (CrosswordPlayRules.isCellLocked(pos.first, pos.second, ui.clues, ui.solvedClueIds)) {
                        return@forEach
                    }
                    val expected = clue.answer.getOrNull(
                        if (clue.direction == ClueDirection.DOWN) pos.first - clue.row else pos.second - clue.col
                    )
                    val got = entries[pos]
                    if (got != null && expected != null && got != expected) {
                        entries.remove(pos)
                    }
                }
                persist()
            }
        }
    }

    fun hint() {
        val clue = selectedClue() ?: return
        if (clue.clueId in ui.solvedClueIds) {
            ui = ui.copy(feedback = "Kjo fjalë është zgjidhur.", feedbackPositive = true)
            return
        }
        val newlyHinted = clue.clueId !in ui.hintedClueIds
        val hinted = ui.hintedClueIds + clue.clueId
        val empty = CrosswordEngine.nextEmptyInClue(clue, entries.toMap())
            ?.takeUnless {
                CrosswordPlayRules.isCellLocked(it.first, it.second, ui.clues, ui.solvedClueIds)
            }
        if (empty != null) {
            val index = if (clue.direction == ClueDirection.DOWN) {
                empty.first - clue.row
            } else {
                empty.second - clue.col
            }
            val letter = clue.answer.getOrNull(index) ?: return
            entries[empty] = letter
            ui = ui.copy(
                selectedCell = empty,
                hintedClueIds = hinted,
                feedback = "Të ndihmoj unë! Një shkronjë u zbulua.",
                feedbackPositive = true,
                sessionHints = ui.sessionHints + if (newlyHinted) 1 else 0
            )
            viewModelScope.launch { persist() }
            return
        }
        val wrong = CrosswordEngine.cellsForClue(clue).firstOrNull { pos ->
            if (CrosswordPlayRules.isCellLocked(pos.first, pos.second, ui.clues, ui.solvedClueIds)) {
                return@firstOrNull false
            }
            val expected = clue.answer.getOrNull(
                if (clue.direction == ClueDirection.DOWN) pos.first - clue.row else pos.second - clue.col
            )
            val got = entries[pos]
            got != null && expected != null && got != expected
        }
        if (wrong != null) {
            val index = if (clue.direction == ClueDirection.DOWN) {
                wrong.first - clue.row
            } else {
                wrong.second - clue.col
            }
            val letter = clue.answer.getOrNull(index) ?: return
            entries[wrong] = letter
            ui = ui.copy(
                selectedCell = wrong,
                hintedClueIds = hinted,
                feedback = "Të ndihmoj unë! U korrigjua një shkronjë.",
                feedbackPositive = true,
                sessionHints = ui.sessionHints + if (newlyHinted) 1 else 0
            )
            viewModelScope.launch { persist() }
        } else {
            ui = ui.copy(
                hintedClueIds = hinted,
                feedback = "Fjala duket e plotë — provo «Kontrollo».",
                feedbackPositive = true,
                sessionHints = ui.sessionHints + if (newlyHinted) 1 else 0
            )
            viewModelScope.launch { persist() }
        }
    }

    fun saveSuggested() {
        val id = ui.suggestSaveWordId ?: ui.revealDefinitionFor ?: return
        viewModelScope.launch {
            bookRepository.save(id, "crossword")
            ui = ui.copy(suggestSaveWordId = null, feedback = "U ruajt te Libri im.", feedbackPositive = true)
        }
    }

    fun dismissCompletionOverlay() {
        ui = ui.copy(showCompletionOverlay = false)
    }
}

@Composable
fun CrosswordPlayScreen(
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onHome: (() -> Unit)? = null,
    onOpenDailyQuiz: (() -> Unit)? = null,
    vm: CrosswordPlayViewModel = hiltViewModel()
) {
    val ui = vm.ui
    val puzzle = ui.puzzle
    val hapticView = rememberHapticView()

    LaunchedEffect(ui.feedback, ui.feedbackPositive, ui.checking) {
        if (!ui.checking && ui.feedback != null) {
            if (ui.feedbackPositive) AlbagramHaptics.success(hapticView)
            else AlbagramHaptics.error(hapticView)
        }
    }

    AlbagramScaffold(
        title = puzzle?.title ?: stringResource(R.string.crossword_title),
        onBack = onBack,
        onHome = onHome
    ) { pad ->
        ScreenBackground {
            when {
                ui.loading -> LoadingState()
                ui.loadFailed || puzzle == null -> EmptyState(
                    text = stringResource(R.string.crossword_missing),
                    actionLabel = stringResource(R.string.retry),
                    onAction = vm::retryLoad
                )
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad.values)
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(AlbagramDimens.screenPadding)
                    ) {
                        CrosswordGrid(
                            width = puzzle.width,
                            height = puzzle.height,
                            cells = ui.cells,
                            entries = vm.entries,
                            selectedClue = vm.selectedClue(),
                            selectedCell = ui.selectedCell,
                            solvedClueIds = ui.solvedClueIds,
                            allClues = ui.clues,
                            onCellClick = vm::selectCell
                        )

                        Spacer(Modifier.height(12.dp))
                        LetterPad(
                            onLetter = {
                                AlbagramHaptics.light(hapticView)
                                vm.typeLetter(it)
                            },
                            onBackspace = {
                                AlbagramHaptics.light(hapticView)
                                vm.clearSelectedCell()
                            }
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = vm::checkAnswer, enabled = !ui.checking) {
                                Text(stringResource(R.string.crossword_check))
                            }
                            OutlinedButton(onClick = vm::hint) {
                                Icon(Icons.Default.Lightbulb, contentDescription = stringResource(R.string.crossword_hint))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.crossword_hint))
                            }
                        }

                        AnimatedVisibility(visible = ui.checking, enter = fadeIn(), exit = fadeOut()) {
                            ThinkingRow()
                        }

                        ui.feedback?.let { msg ->
                            Text(
                                msg,
                                color = if (ui.feedbackPositive) Leaf else Coral,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        SessionSummaryCard(
                            title = stringResource(R.string.session_summary_title),
                            lines = listOf(
                                stringResource(
                                    R.string.crossword_session_progress,
                                    ui.solvedClueIds.size,
                                    ui.clues.size
                                ),
                                stringResource(R.string.crossword_session_points, ui.sessionPoints),
                                stringResource(R.string.crossword_session_hints, ui.sessionHints)
                            )
                        )

                        ui.definitionText?.let { def ->
                            Text(
                                "Përkufizimi: $def",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            ui.revealDefinitionFor?.let { id ->
                                Text(
                                    stringResource(R.string.ese_view_word),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onOpenWord(id) }
                                        .padding(top = 4.dp)
                                )
                            }
                        }

                        if (ui.suggestSaveWordId != null) {
                            OutlinedButton(onClick = vm::saveSuggested, modifier = Modifier.padding(top = 8.dp)) {
                                Text(stringResource(R.string.crossword_save))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.crossword_across), style = MaterialTheme.typography.titleLarge)
                        ui.clues.filter { it.direction == ClueDirection.ACROSS }.forEach { clue ->
                            ClueRow(clue, ui.selectedClueId == clue.clueId, clue.clueId in ui.solvedClueIds) {
                                vm.selectClue(clue.clueId)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.crossword_down), style = MaterialTheme.typography.titleLarge)
                        ui.clues.filter { it.direction == ClueDirection.DOWN }.forEach { clue ->
                            ClueRow(clue, ui.selectedClueId == clue.clueId, clue.clueId in ui.solvedClueIds) {
                                vm.selectClue(clue.clueId)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (ui.showCompletionOverlay) {
        AlertDialog(
            onDismissRequest = vm::dismissCompletionOverlay,
            title = { Text(stringResource(R.string.crossword_complete_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.crossword_complete_body, ui.sessionPoints))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.crossword_complete_hints, ui.sessionHints))
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.dismissCompletionOverlay()
                    onOpenDailyQuiz?.invoke()
                }) {
                    Text(stringResource(R.string.crossword_complete_quiz_cta))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissCompletionOverlay) {
                    Text(stringResource(R.string.badge_celebration_dismiss))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterPad(
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit
) {
    val letters = listOf(
        "A", "B", "C", "Ç", "D", "E", "Ë", "F", "G", "H", "I", "J", "K", "L",
        "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "X", "Y", "Z"
    )
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
            .background(scheme.surface.copy(alpha = 0.92f))
            .padding(10.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            letters.forEach { letter ->
                Surface(
                    onClick = { onLetter(letter.first()) },
                    shape = RoundedCornerShape(8.dp),
                    color = scheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(width = 34.dp, height = 36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(letter, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Surface(
                onClick = onBackspace,
                shape = RoundedCornerShape(8.dp),
                color = scheme.errorContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(width = 52.dp, height = 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.cd_clear_letter),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingRow() {
    val transition = rememberInfiniteTransition(label = "think")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "a"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .alpha(alpha)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Gold)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.crossword_thinking))
    }
}

@Composable
private fun ClueRow(
    clue: CrosswordClue,
    selected: Boolean,
    solved: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(AlbagramDimens.chipRadius))
            .background(
                when {
                    selected -> scheme.secondaryContainer.copy(alpha = 0.85f)
                    solved -> scheme.tertiaryContainer.copy(alpha = 0.55f)
                    else -> scheme.surface.copy(alpha = 0.9f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${clue.number}. ${clue.clueText}" + if (solved) " ✓" else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CrosswordGrid(
    width: Int,
    height: Int,
    cells: List<GridCellSeed>,
    entries: Map<Pair<Int, Int>, Char>,
    selectedClue: CrosswordClue?,
    selectedCell: Pair<Int, Int>?,
    solvedClueIds: Set<String>,
    allClues: List<CrosswordClue>,
    onCellClick: (Int, Int) -> Unit
) {
    val cellMap = cells.associateBy { it.row to it.col }
    val highlight = selectedClue?.let { CrosswordEngine.cellsForClue(it).toSet() }.orEmpty()
    val solvedCells = allClues
        .filter { it.clueId in solvedClueIds }
        .flatMap { CrosswordEngine.cellsForClue(it) }
        .toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Forest.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        for (r in 0 until height) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until width) {
                    val cell = cellMap[r to c]
                    val isBlock = cell?.block != false
                    val pos = r to c
                    val inClue = !isBlock && pos in highlight
                    val isCursor = !isBlock && pos == selectedCell
                    val isSolved = !isBlock && pos in solvedCells
                    val scale by animateFloatAsState(
                        targetValue = if (isSolved) 1.05f else 1f,
                        animationSpec = tween(350),
                        label = "solvedScale"
                    )
                    val bg = when {
                        isBlock -> Forest.copy(alpha = 0.85f)
                        isSolved -> Leaf.copy(alpha = 0.28f)
                        isCursor -> Gold.copy(alpha = 0.45f)
                        inClue -> Gold.copy(alpha = 0.22f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg)
                            .then(
                                if (isBlock) {
                                    Modifier
                                } else {
                                    Modifier
                                        .border(
                                            width = if (isCursor) 2.dp else 1.dp,
                                            color = when {
                                                isCursor -> Gold
                                                isSolved -> Leaf
                                                inClue -> Gold.copy(alpha = 0.7f)
                                                else -> Forest.copy(alpha = 0.35f)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .semantics {
                                            contentDescription = "Rreshti ${r + 1}, kolona ${c + 1}" +
                                                (entries[r to c]?.let { ", shkronja $it" } ?: "")
                                        }
                                        .clickable { onCellClick(r, c) }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isBlock) {
                            cell?.number?.let { num ->
                                Text(
                                    "$num",
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(2.dp)
                                )
                            }
                            Text(
                                entries[r to c]?.toString().orEmpty(),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                color = if (isSolved) Forest else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
