package com.albagram.app.ui.rima

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.data.repository.RhymeRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.domain.progress.ProgressMilestones
import com.albagram.app.domain.rhyme.RhymeQuizGenerator
import com.albagram.app.domain.rhyme.RhymeQuizRound
import com.albagram.app.ui.components.AlbagramHaptics
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.LoadingState
import com.albagram.app.ui.components.ModuleScreenHeader
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SessionSummaryCard
import com.albagram.app.ui.components.WordListItem
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.theme.AlbagramDimens
import com.albagram.app.ui.theme.Coral
import com.albagram.app.ui.theme.Leaf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RimaMode { SEARCH, QUIZ }

@HiltViewModel
class RimaViewModel @Inject constructor(
    private val rhymeRepository: RhymeRepository,
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    var mode by mutableStateOf(RimaMode.SEARCH)
        private set
    var input by mutableStateOf("")
        private set
    var hasSearched by mutableStateOf(false)
        private set
    var searching by mutableStateOf(false)
        private set
    var blankError by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set

    var quizStarted by mutableStateOf(false)
        private set
    var quizFinished by mutableStateOf(false)
        private set
    var quizRoundIndex by mutableIntStateOf(0)
        private set
    var quizScore by mutableIntStateOf(0)
        private set
    var currentRound by mutableStateOf<RhymeQuizRound?>(null)
        private set
    var quizTargets by mutableStateOf<List<WordEntity>>(emptyList())
        private set
    var lastQuizCorrect by mutableStateOf<Boolean?>(null)
        private set
    var quizLoadFailed by mutableStateOf(false)
        private set
    var isStarting by mutableStateOf(false)
        private set
    private var quizCorpus: List<WordEntity> = emptyList()

    private val resultsFlow = MutableStateFlow<List<WordEntity>>(emptyList())
    val results = resultsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectMode(value: RimaMode) {
        mode = value
    }

    fun updateInput(value: String) {
        input = value
        blankError = false
        searchError = null
    }

    fun search() {
        if (input.isBlank()) {
            blankError = true
            hasSearched = false
            resultsFlow.value = emptyList()
            return
        }
        viewModelScope.launch {
            searching = true
            hasSearched = true
            blankError = false
            searchError = null
            resultsFlow.value = runCatching { rhymeRepository.findRhymes(input) }
                .onFailure { searchError = "Kërkimi dështoi. Provo përsëri." }
                .getOrDefault(emptyList())
            searching = false
        }
    }

    fun startQuiz() {
        if (isStarting) return
        viewModelScope.launch {
            isStarting = true
            try {
                quizLoadFailed = false
                val corpus = wordRepository.getAllGlossary()
                quizCorpus = corpus
                quizTargets = RhymeQuizGenerator.pickTargets(corpus, count = 5)
                if (quizTargets.isEmpty()) {
                    quizLoadFailed = true
                    return@launch
                }
                quizStarted = true
                quizFinished = false
                quizRoundIndex = 0
                quizScore = 0
                lastQuizCorrect = null
                loadQuizRound(corpus)
            } finally {
                isStarting = false
            }
        }
    }

    private fun loadQuizRound(corpus: List<WordEntity>) {
        val target = quizTargets.getOrNull(quizRoundIndex) ?: run {
            finishQuiz()
            return
        }
        currentRound = RhymeQuizGenerator.buildRound(target, corpus)
        if (currentRound == null) {
            if (quizRoundIndex >= quizTargets.lastIndex) finishQuiz() else {
                quizRoundIndex += 1
                loadQuizRound(corpus)
            }
        }
    }

    fun answerQuiz(optionIndex: Int) {
        val round = currentRound ?: return
        if (lastQuizCorrect != null) return
        val correct = optionIndex == round.correctIndex
        lastQuizCorrect = correct
        viewModelScope.launch {
            if (correct) {
                quizScore += 5
                progressRepository.addScore(5)
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            lastQuizCorrect = null
            if (quizRoundIndex >= quizTargets.lastIndex) {
                finishQuiz()
            } else {
                quizRoundIndex += 1
                loadQuizRound(quizCorpus)
            }
        }
    }

    private fun finishQuiz() {
        quizFinished = true
        quizStarted = false
        currentRound = null
        viewModelScope.launch {
            if (quizScore >= 15) {
                progressRepository.awardBadge(ProgressMilestones.BADGE_RIMUES)
            }
        }
    }
}

@Composable
fun RimaScreen(
    contentPadding: PaddingValues,
    onOpenWord: (String) -> Unit,
    onHome: (() -> Unit)? = null,
    vm: RimaViewModel = hiltViewModel()
) {
    val results by vm.results.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val hapticView = rememberHapticView()

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(AlbagramDimens.screenPadding)
        ) {
            ModuleScreenHeader(
                title = stringResource(R.string.rima_title),
                subtitle = stringResource(R.string.rima_subtitle),
                onHome = onHome
            )
            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = vm.mode == RimaMode.SEARCH,
                    onClick = { vm.selectMode(RimaMode.SEARCH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(R.string.rima_mode_search)) }
                SegmentedButton(
                    selected = vm.mode == RimaMode.QUIZ,
                    onClick = { vm.selectMode(RimaMode.QUIZ) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(R.string.rima_mode_quiz)) }
            }
            Spacer(Modifier.height(AlbagramDimens.sectionGap))

            when (vm.mode) {
                RimaMode.SEARCH -> RimaSearchContent(vm, results, focus, onOpenWord)
                RimaMode.QUIZ -> RimaQuizContent(vm, hapticView)
            }
        }
    }
}

@Composable
private fun RimaSearchContent(
    vm: RimaViewModel,
    results: List<WordEntity>,
    focus: androidx.compose.ui.focus.FocusManager,
    onOpenWord: (String) -> Unit
) {
    OutlinedTextField(
        value = vm.input,
        onValueChange = vm::updateInput,
        modifier = Modifier.fillMaxWidth().imePadding(),
        placeholder = { Text(stringResource(R.string.rima_hint)) },
        singleLine = true,
        isError = vm.blankError,
        supportingText = if (vm.blankError) {
            { Text(stringResource(R.string.rima_blank)) }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focus.clearFocus()
            vm.search()
        })
    )
    Spacer(Modifier.height(AlbagramDimens.tightGap))
    Button(onClick = vm::search, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.rima_search))
    }
    Spacer(Modifier.height(AlbagramDimens.sectionGap))
    if (vm.hasSearched) {
        SessionSummaryCard(
            title = stringResource(R.string.session_summary_title),
            lines = listOf(
                stringResource(R.string.rima_summary_results, results.size),
                stringResource(R.string.rima_summary_next_step)
            )
        )
        Spacer(Modifier.height(AlbagramDimens.tightGap))
    }
    vm.searchError?.let {
        Text(text = it, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(AlbagramDimens.tightGap))
    }
    when {
        vm.searching -> LoadingState(text = stringResource(R.string.rima_searching))
        !vm.hasSearched -> EmptyState(stringResource(R.string.rima_idle))
        results.isEmpty() -> EmptyState(stringResource(R.string.rima_empty))
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.wordId }) { word ->
                    WordListItem(word = word, onClick = { onOpenWord(word.wordId) })
                }
            }
        }
    }
}

@Composable
private fun RimaQuizContent(vm: RimaViewModel, hapticView: android.view.View?) {
    if (vm.quizLoadFailed) {
        EmptyState(stringResource(R.string.rima_quiz_failed))
        return
    }
    if (!vm.quizStarted && !vm.quizFinished) {
        EmptyState(stringResource(R.string.rima_quiz_intro))
        Spacer(Modifier.height(12.dp))
        Button(onClick = vm::startQuiz, enabled = !vm.isStarting, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.rima_quiz_start))
        }
        return
    }
    if (vm.quizFinished) {
        SessionSummaryCard(
            title = stringResource(R.string.session_summary_title),
            lines = listOf(
                stringResource(R.string.rima_quiz_score, vm.quizScore),
                stringResource(R.string.rima_quiz_rounds, vm.quizTargets.size)
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = vm::startQuiz) { Text(stringResource(R.string.rima_quiz_again)) }
        return
    }
    val round = vm.currentRound ?: return
    val bounce by animateFloatAsState(
        targetValue = if (vm.lastQuizCorrect == true) 1.08f else 1f,
        animationSpec = spring(),
        label = "quizBounce"
    )
    LaunchedEffect(vm.quizRoundIndex, vm.lastQuizCorrect) {
        when (vm.lastQuizCorrect) {
            true -> hapticView?.let { AlbagramHaptics.success(it) }
            false -> hapticView?.let { AlbagramHaptics.error(it) }
            null -> Unit
        }
    }

    Text(
        stringResource(R.string.rima_quiz_round, vm.quizRoundIndex + 1, vm.quizTargets.size),
        style = MaterialTheme.typography.labelLarge
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.rima_quiz_prompt, round.promptTerm),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    round.options.forEachIndexed { index, option ->
        OutlinedButton(
            onClick = { vm.answerQuiz(index) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = vm.lastQuizCorrect == null
        ) {
            Text(option)
        }
    }
    vm.lastQuizCorrect?.let { ok ->
        Text(
            if (ok) stringResource(R.string.konkursi_correct) else stringResource(R.string.konkursi_wrong),
            color = if (ok) Leaf else Coral,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.scale(bounce)
        )
    }
}
