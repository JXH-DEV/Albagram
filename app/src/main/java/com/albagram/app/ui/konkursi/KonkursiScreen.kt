package com.albagram.app.ui.konkursi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.local.KonkursiStats
import com.albagram.app.data.local.KonkursiStatsStore
import com.albagram.app.data.local.entity.QuizQuestionEntity
import com.albagram.app.data.repository.BookRepository
import com.albagram.app.data.repository.DailyChallengeRepository
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.data.repository.QuizRepository
import com.albagram.app.domain.daily.DailyChallengeKeys
import com.albagram.app.domain.quiz.AdvanceLock
import com.albagram.app.domain.quiz.QuizAdvanceResult
import com.albagram.app.ui.components.AlbagramHaptics
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.ModuleScreenHeader
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SessionSummaryCard
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.theme.AlbagramDimens
import com.albagram.app.ui.theme.Coral
import com.albagram.app.ui.theme.Forest
import com.albagram.app.ui.theme.Leaf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DAILY_SCORE_THRESHOLD = 30

@HiltViewModel
class KonkursiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    private val progressRepository: ProgressRepository,
    private val bookRepository: BookRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val statsStore: KonkursiStatsStore
) : ViewModel() {
    data class QuestionUi(
        val question: QuizQuestionEntity,
        val options: List<String>
    )

    val dailyMode: Boolean = savedStateHandle.get<Boolean>("daily") ?: false

    var started by mutableStateOf(false)
        private set
    var finished by mutableStateOf(false)
        private set
    var questions by mutableStateOf<List<QuizQuestionEntity>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var secondsLeft by mutableIntStateOf(0)
        private set
    var score by mutableIntStateOf(0)
        private set
    var lastResult by mutableStateOf<QuizAdvanceResult?>(null)
        private set
    var options by mutableStateOf<List<String>>(emptyList())
        private set
    var startFailed by mutableStateOf(false)
        private set
    var suggestedSaveWordId by mutableStateOf<String?>(null)
        private set
    val pendingSaveWordIds = mutableStateListOf<String>()
    var correctCount by mutableIntStateOf(0)
        private set
    var wrongCount by mutableIntStateOf(0)
        private set
    var timeoutCount by mutableIntStateOf(0)
        private set
    var savedCount by mutableIntStateOf(0)
        private set
    var currentQuestionUi by mutableStateOf<QuestionUi?>(null)
        private set
    var personalStats by mutableStateOf(KonkursiStats())
        private set
    var badgeCount by mutableIntStateOf(0)
        private set
    var isStarting by mutableStateOf(false)
        private set

    val progress = progressRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var timerJob: Job? = null
    private var advanceJob: Job? = null
    private val advanceLock = AdvanceLock()

    init {
        viewModelScope.launch { personalStats = statsStore.load() }
        viewModelScope.launch {
            progressRepository.observe().collect { entity ->
                badgeCount = entity?.let { progressRepository.parseBadges(it).size } ?: 0
            }
        }
        if (dailyMode) {
            startDaily()
        }
    }

    fun start() {
        if (isStarting) return
        viewModelScope.launch {
            isStarting = true
            try {
                startFailed = false
                suggestedSaveWordId = null
                pendingSaveWordIds.clear()
                currentQuestionUi = null
                questions = quizRepository.getShuffledQuestions().take(8)
                if (questions.isEmpty()) {
                    startFailed = true
                    return@launch
                }
                beginSession()
            } finally {
                isStarting = false
            }
        }
    }

    fun startDaily() {
        if (isStarting) return
        viewModelScope.launch {
            isStarting = true
            try {
                startFailed = false
                suggestedSaveWordId = null
                pendingSaveWordIds.clear()
                currentQuestionUi = null
                val daily = dailyChallengeRepository.forDate()
                val ids = daily.quizQuestionIds
                questions = quizRepository.getDailyQuestions(ids)
                    .filter { quizRepository.isQuestionUsable(it) }
                if (questions.isEmpty()) {
                    startFailed = true
                    return@launch
                }
                beginSession()
            } finally {
                isStarting = false
            }
        }
    }

    private fun beginSession() {
        started = true
        finished = false
        index = 0
        score = 0
        correctCount = 0
        wrongCount = 0
        timeoutCount = 0
        savedCount = 0
        loadQuestion()
    }

    private fun loadQuestion() {
        timerJob?.cancel()
        advanceJob?.cancel()
        advanceLock.unlock()
        lastResult = null
        suggestedSaveWordId = pendingSaveWordIds.firstOrNull()
        val q = questions.getOrNull(index) ?: run {
            finish()
            return
        }
        val parsedOptions = quizRepository.parseOptions(q)
        if (!quizRepository.isQuestionUsable(q)) {
            lockAndAdvance(QuizAdvanceResult.TIMEOUT) {
                timeoutCount += 1
            }
            return
        }
        options = parsedOptions
        currentQuestionUi = QuestionUi(q, parsedOptions)
        secondsLeft = q.timeLimitSeconds
        timerJob = viewModelScope.launch {
            while (isActive && secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            if (isActive && secondsLeft <= 0 && lastResult == null) {
                onTimeout(q)
            }
        }
    }

    private fun lockAndAdvance(result: QuizAdvanceResult, onLocked: suspend () -> Unit) {
        if (!advanceLock.tryLock()) return
        lastResult = result
        timerJob?.cancel()
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            onLocked()
            delay(900)
            next()
        }
    }

    private fun onTimeout(q: QuizQuestionEntity) {
        lockAndAdvance(QuizAdvanceResult.TIMEOUT) {
            progressRepository.resetStreak()
            timeoutCount += 1
            q.relatedWordId?.let { offerSave(it) }
        }
    }

    fun answer(optionIndex: Int) {
        val q = currentQuestionUi?.question ?: questions.getOrNull(index) ?: return
        if (lastResult != null) return
        if (optionIndex == q.correctIndex) {
            val gained = 10 + secondsLeft
            lockAndAdvance(QuizAdvanceResult.CORRECT) {
                score += gained
                correctCount += 1
                progressRepository.addScore(gained)
                progressRepository.bumpStreak()
            }
        } else {
            lockAndAdvance(QuizAdvanceResult.WRONG) {
                wrongCount += 1
                progressRepository.resetStreak()
                q.relatedWordId?.let { offerSave(it) }
            }
        }
    }

    private fun offerSave(wordId: String) {
        if (wordId !in pendingSaveWordIds) pendingSaveWordIds += wordId
        suggestedSaveWordId = wordId
    }

    fun saveSuggestedWord() {
        val id = suggestedSaveWordId ?: pendingSaveWordIds.firstOrNull() ?: return
        viewModelScope.launch {
            bookRepository.save(id, "quiz")
            pendingSaveWordIds.remove(id)
            suggestedSaveWordId = pendingSaveWordIds.firstOrNull()
            savedCount += 1
        }
    }

    private fun next() {
        if (index >= questions.lastIndex) {
            finish()
        } else {
            index += 1
            loadQuestion()
        }
    }

    private fun finish() {
        timerJob?.cancel()
        advanceJob?.cancel()
        finished = true
        started = false
        currentQuestionUi = null
        viewModelScope.launch {
            statsStore.recordSession(score, correctCount, wrongCount, timeoutCount)
            personalStats = statsStore.load()
            if (score >= 60) progressRepository.awardBadge("Kampion i konkursit")
            progressRepository.awardBadge("Pjesëmarrës")
            if (dailyMode && score >= DAILY_SCORE_THRESHOLD) {
                progressRepository.markDailyComplete(DailyChallengeKeys.QUIZ)
            }
        }
    }
}

@Composable
fun KonkursiScreen(
    contentPadding: PaddingValues,
    onHome: (() -> Unit)? = null,
    onOpenDailyQuiz: (() -> Unit)? = null,
    vm: KonkursiViewModel = hiltViewModel()
) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val hapticView = rememberHapticView()

    LaunchedEffect(vm.lastResult) {
        when (vm.lastResult) {
            QuizAdvanceResult.CORRECT -> AlbagramHaptics.success(hapticView)
            QuizAdvanceResult.WRONG, QuizAdvanceResult.TIMEOUT -> AlbagramHaptics.error(hapticView)
            null -> Unit
        }
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(AlbagramDimens.screenPadding)
        ) {
            ModuleScreenHeader(
                title = if (vm.dailyMode) {
                    stringResource(R.string.konkursi_daily_title)
                } else {
                    stringResource(R.string.konkursi_title)
                },
                subtitle = stringResource(R.string.konkursi_subtitle),
                onHome = onHome
            )
            Spacer(Modifier.height(AlbagramDimens.tightGap))
            progress?.let {
                Text(
                    "${stringResource(R.string.home_score)}: ${it.score} · " +
                        "${stringResource(R.string.home_quiz_streak)}: ${it.streak}",
                    fontWeight = FontWeight.Medium
                )
            }

            if (vm.startFailed) {
                Text(
                    stringResource(R.string.konkursi_no_questions),
                    color = Coral,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!vm.started && !vm.finished) {
                Spacer(Modifier.height(AlbagramDimens.sectionGap))
                PersonalStatsPanel(
                    stats = vm.personalStats,
                    quizStreak = progress?.streak ?: 0,
                    badgeCount = vm.badgeCount
                )
                Spacer(Modifier.height(AlbagramDimens.sectionGap))
                if (!vm.dailyMode) {
                    Button(
                        onClick = vm::start,
                        enabled = !vm.isStarting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.konkursi_start))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOpenDailyQuiz?.invoke() ?: vm.startDaily() },
                        enabled = !vm.isStarting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.konkursi_daily_start))
                    }
                }
            }

            if (vm.finished) {
                Spacer(Modifier.height(AlbagramDimens.sectionGap))
                Text(
                    stringResource(R.string.konkursi_result, vm.score),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Forest
                )
                SessionSummaryCard(
                    title = stringResource(R.string.session_summary_title),
                    lines = listOf(
                        stringResource(R.string.konkursi_summary_score, vm.score),
                        stringResource(
                            R.string.konkursi_summary_answers,
                            vm.correctCount,
                            vm.wrongCount + vm.timeoutCount
                        ),
                        stringResource(R.string.konkursi_summary_saved, vm.savedCount)
                    )
                )
                if (vm.pendingSaveWordIds.isNotEmpty() || vm.suggestedSaveWordId != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.konkursi_save_pending), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = vm::saveSuggestedWord) {
                        Text(stringResource(R.string.konkursi_save_word))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (vm.dailyMode) vm.startDaily() else vm.start()
                }) {
                    Text(stringResource(R.string.konkursi_again))
                }
            }

            if (vm.started && !vm.finished) {
                val q = vm.questions.getOrNull(vm.index)
                if (q == null) {
                    EmptyState(stringResource(R.string.konkursi_no_questions))
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text("Pyetja ${vm.index + 1}/${vm.questions.size}", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${vm.secondsLeft}s",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (vm.secondsLeft <= 5) Coral else Forest
                    )
                    LinearProgressIndicator(
                        progress = {
                            val total = q.timeLimitSeconds.coerceAtLeast(1).toFloat()
                            vm.secondsLeft / total
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(q.prompt, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    vm.options.forEachIndexed { i, option ->
                        OutlinedButton(
                            onClick = { vm.answer(i) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            enabled = vm.lastResult == null
                        ) {
                            Text(option)
                        }
                    }
                    val correct = vm.lastResult == QuizAdvanceResult.CORRECT
                    val bounce by animateFloatAsState(
                        targetValue = if (correct) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "correctBounce"
                    )
                    AnimatedVisibility(visible = vm.lastResult != null) {
                        Text(
                            quizResultText(vm.lastResult),
                            color = if (correct) Leaf else Coral,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.scale(bounce)
                        )
                    }
                    if (vm.suggestedSaveWordId != null || vm.pendingSaveWordIds.isNotEmpty()) {
                        OutlinedButton(
                            onClick = vm::saveSuggestedWord,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.konkursi_save_word))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalStatsPanel(stats: KonkursiStats, quizStreak: Int, badgeCount: Int) {
    SessionSummaryCard(
        title = stringResource(R.string.konkursi_stats_title),
        lines = listOf(
            stringResource(R.string.konkursi_stats_best, stats.bestScore),
            stringResource(
                R.string.konkursi_stats_last,
                stats.lastCorrect,
                stats.lastWrong,
                stats.lastTimeout
            ),
            stringResource(R.string.home_quiz_streak) + ": $quizStreak",
            stringResource(R.string.konkursi_stats_badges, badgeCount)
        )
    )
}

@Composable
private fun quizResultText(result: QuizAdvanceResult?): String {
    return when (result) {
        QuizAdvanceResult.CORRECT -> stringResource(R.string.konkursi_correct)
        QuizAdvanceResult.WRONG -> stringResource(R.string.konkursi_wrong)
        QuizAdvanceResult.TIMEOUT -> stringResource(R.string.konkursi_timeout)
        null -> ""
    }
}
