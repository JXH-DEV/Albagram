package com.albagram.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.BuildConfig
import com.albagram.app.R
import com.albagram.app.data.local.entity.UserProgressEntity
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.data.repository.BookRepository
import com.albagram.app.data.repository.CrosswordRepository
import com.albagram.app.data.repository.DailyChallengeRepository
import com.albagram.app.data.repository.EssayRepository
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.domain.daily.DailyChallengeKeys
import com.albagram.app.domain.daily.DailyChallengeSet
import com.albagram.app.domain.progress.ProgressMilestones
import com.albagram.app.ui.components.BadgeCelebrationDialog
import com.albagram.app.ui.components.ModuleCard
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.navigation.Routes
import com.albagram.app.ui.theme.AlbagramDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeProgressUi(
    val score: Int = 0,
    val quizStreak: Int = 0,
    val dailyVisitStreak: Int = 0,
    val badges: List<String> = emptyList(),
    val scoreGoalRemaining: Int = 0,
    val scoreGoalTarget: Int = 50
)

data class HomeDailyRow(
    val key: String,
    val label: String,
    val done: Boolean
)

data class HomeResumeUi(
    val crosswordPuzzleId: String? = null,
    val essayPromptId: String? = null
)

data class HomeModuleStatsUi(
    val crosswordsDone: Int = 0,
    val crosswordsTotal: Int = 0,
    val essaysDone: Int = 0,
    val essaysTotal: Int = 0,
    val savedWords: Int = 0
)

data class HomeUiState(
    val progress: HomeProgressUi = HomeProgressUi(),
    val daily: DailyChallengeSet? = null,
    val dailyRows: List<HomeDailyRow> = emptyList(),
    val resume: HomeResumeUi = HomeResumeUi(),
    val moduleStats: HomeModuleStatsUi = HomeModuleStatsUi(),
    val weeklyWords: List<WordEntity> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val crosswordRepository: CrosswordRepository,
    private val essayRepository: EssayRepository,
    private val bookRepository: BookRepository,
    private val wordRepository: WordRepository
) : ViewModel() {
    val badgeUnlocks = progressRepository.badgeUnlocks

    val state = combine(
        progressRepository.observe(),
        bookRepository.observeCount()
    ) { progressEntity, savedCount ->
        buildState(progressEntity, savedCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            progressRepository.recordDailyVisit()
        }
    }

    private suspend fun buildState(entity: UserProgressEntity?, savedCount: Int): HomeUiState {
        val p = entity ?: UserProgressEntity()
        val today = LocalDate.now()
        val daily = dailyChallengeRepository.forDate(today)
        val completed = progressRepository.dailyCompletedKeys(p, today)
        val scoreGoal = ProgressMilestones.nextScoreGoal(p.score)
        val crosswordDone = crosswordRepository.countCompleted()
        val crosswordTotal = crosswordRepository.countPuzzles()
        val essaysDone = essayRepository.countCompleted()
        val essaysTotal = essayRepository.countPrompts()
        val resumeCrossword = crosswordRepository.getLatestInProgress()?.puzzleId
        val resumeEssay = essayRepository.getLatestInProgressDraft()?.promptId
        val weeklyWords = wordRepository.getByIds(daily.weeklyWordIds)

        return HomeUiState(
            progress = HomeProgressUi(
                score = p.score,
                quizStreak = p.streak,
                dailyVisitStreak = p.dailyVisitStreak,
                badges = progressRepository.parseBadges(p),
                scoreGoalRemaining = scoreGoal.remaining,
                scoreGoalTarget = scoreGoal.target
            ),
            daily = daily,
            dailyRows = listOf(
                HomeDailyRow(DailyChallengeKeys.WORD, "Fjalë e ditës", DailyChallengeKeys.WORD in completed),
                HomeDailyRow(DailyChallengeKeys.CROSSWORD, "Fjalëkryqi i ditës", DailyChallengeKeys.CROSSWORD in completed),
                HomeDailyRow(DailyChallengeKeys.QUIZ, "Kuiz i ditës", DailyChallengeKeys.QUIZ in completed),
                HomeDailyRow(DailyChallengeKeys.ESE, "Ese e ditës", DailyChallengeKeys.ESE in completed)
            ),
            resume = HomeResumeUi(
                crosswordPuzzleId = resumeCrossword,
                essayPromptId = resumeEssay
            ),
            moduleStats = HomeModuleStatsUi(
                crosswordsDone = crosswordDone,
                crosswordsTotal = crosswordTotal,
                essaysDone = essaysDone,
                essaysTotal = essaysTotal,
                savedWords = savedCount
            ),
            weeklyWords = weeklyWords
        )
    }
}

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenModule: (String) -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenDailyCrossword: (String) -> Unit,
    onOpenDailyQuiz: () -> Unit,
    onOpenDailyWord: (String) -> Unit,
    onResumeCrossword: (String) -> Unit,
    onResumeEssay: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val progress = ui.progress
    val scheme = MaterialTheme.colorScheme
    val fade = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0.85f) }
    val hapticView = rememberHapticView()
    var celebratingBadge by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(700))
    }
    LaunchedEffect(progress.badges) {
        if (progress.badges.isNotEmpty()) {
            badgeScale.snapTo(0.85f)
            badgeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    LaunchedEffect(Unit) {
        vm.badgeUnlocks.collect { badge ->
            celebratingBadge = badge
        }
    }

    BadgeCelebrationDialog(
        badge = celebratingBadge,
        onDismiss = { celebratingBadge = null },
        hapticView = hapticView
    )

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AlbagramDimens.screenPadding)
                .alpha(fade.value)
        ) {
            Spacer(Modifier.height(12.dp))
            HeroHeader()
            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            ProgressStrip(
                score = progress.score,
                quizStreak = progress.quizStreak,
                dailyVisitStreak = progress.dailyVisitStreak,
                badges = progress.badges,
                badgeScale = badgeScale.value
            )

            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            DailyChallengeCard(
                rows = ui.dailyRows,
                onWord = { ui.daily?.wordId?.let(onOpenDailyWord) },
                onCrossword = { ui.daily?.puzzleId?.let(onOpenDailyCrossword) },
                onQuiz = onOpenDailyQuiz,
                onEse = { ui.daily?.essayPromptId?.let(onResumeEssay) }
            )

            if (ui.resume.crosswordPuzzleId != null || ui.resume.essayPromptId != null) {
                Spacer(Modifier.height(AlbagramDimens.sectionGap))
                ResumeSection(
                    crosswordId = ui.resume.crosswordPuzzleId,
                    essayId = ui.resume.essayPromptId,
                    onCrossword = onResumeCrossword,
                    onEssay = onResumeEssay
                )
            }

            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            ModuleStatsStrip(stats = ui.moduleStats)

            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            Text(stringResource(R.string.home_modules), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            ModuleCard(
                title = stringResource(R.string.nav_crossword),
                subtitle = stringResource(R.string.module_crossword_sub),
                icon = Icons.Default.Extension,
                onClick = { onOpenModule(Routes.Crossword.route) },
                emphasized = true,
                modifier = Modifier.padding(bottom = AlbagramDimens.itemGap)
            )
            ModuleCard(
                title = stringResource(R.string.nav_rima),
                subtitle = stringResource(R.string.module_rima_sub),
                icon = Icons.Default.MusicNote,
                onClick = { onOpenModule(Routes.Rima.route) },
                modifier = Modifier.padding(bottom = AlbagramDimens.itemGap)
            )
            ModuleCard(
                title = stringResource(R.string.nav_ese),
                subtitle = stringResource(R.string.module_ese_sub),
                icon = Icons.Default.EditNote,
                onClick = { onOpenModule(Routes.Ese.route) },
                modifier = Modifier.padding(bottom = AlbagramDimens.itemGap)
            )
            ModuleCard(
                title = stringResource(R.string.nav_konkursi),
                subtitle = stringResource(R.string.module_konkursi_sub),
                icon = Icons.Default.EmojiEvents,
                onClick = { onOpenModule(Routes.Konkursi.create()) },
                modifier = Modifier.padding(bottom = AlbagramDimens.itemGap)
            )
            ModuleCard(
                title = stringResource(R.string.nav_libri),
                subtitle = stringResource(R.string.module_libri_sub),
                icon = Icons.Default.AutoStories,
                onClick = { onOpenModule(Routes.Libri.route) },
                modifier = Modifier.padding(bottom = AlbagramDimens.itemGap)
            )

            Text(
                stringResource(R.string.home_weekly_words),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.weeklyWords.forEach { word ->
                    AssistChip(
                        onClick = { onOpenDailyWord(word.wordId) },
                        label = { Text(word.term) }
                    )
                }
            }

            TextButton(onClick = onOpenGlossary) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = stringResource(R.string.cd_open_glossary))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_open_glossary))
            }
            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            Text(
                stringResource(R.string.home_about, stringResource(R.string.app_name), BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.home_offline),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroHeader() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.heroRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary,
                        scheme.tertiary.copy(alpha = 0.9f),
                        scheme.primary.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.ic_albagram_logo),
                contentDescription = stringResource(R.string.cd_logo),
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.secondary.copy(alpha = 0.25f))
                    .padding(12.dp)
            )
            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = scheme.onPrimary
            )
            Spacer(Modifier.height(AlbagramDimens.tightGap))
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun DailyChallengeCard(
    rows: List<HomeDailyRow>,
    onWord: () -> Unit,
    onCrossword: () -> Unit,
    onQuiz: () -> Unit,
    onEse: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
            .background(scheme.surface.copy(alpha = 0.92f))
            .padding(AlbagramDimens.sectionGap)
    ) {
        Text(
            stringResource(R.string.home_daily_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.primary
        )
        Spacer(Modifier.height(8.dp))
        rows.forEach { row ->
            val onClick = when (row.key) {
                DailyChallengeKeys.WORD -> onWord
                DailyChallengeKeys.CROSSWORD -> onCrossword
                DailyChallengeKeys.QUIZ -> onQuiz
                DailyChallengeKeys.ESE -> onEse
                else -> null
            }
            DailyRowItem(row = row, onClick = onClick)
        }
    }
}

@Composable
private fun DailyRowItem(row: HomeDailyRow, onClick: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (row.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (row.done) scheme.primary else scheme.onSurfaceVariant
        )
        Text(row.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (onClick != null && !row.done) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = scheme.primary)
        }
    }
}

@Composable
private fun ResumeSection(
    crosswordId: String?,
    essayId: String?,
    onCrossword: (String) -> Unit,
    onEssay: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
            .background(scheme.primary.copy(alpha = 0.06f))
            .padding(AlbagramDimens.sectionGap)
    ) {
        Text(stringResource(R.string.home_resume_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        crosswordId?.let { id ->
            TextButton(onClick = { onCrossword(id) }) {
                Icon(Icons.Default.Extension, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_resume_crossword))
            }
        }
        essayId?.let { id ->
            TextButton(onClick = { onEssay(id) }) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_resume_essay))
            }
        }
    }
}

@Composable
private fun ModuleStatsStrip(stats: HomeModuleStatsUi) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
            .background(scheme.surface.copy(alpha = 0.92f))
            .padding(AlbagramDimens.sectionGap)
    ) {
        Text(stringResource(R.string.home_module_stats), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.home_stat_crosswords, stats.crosswordsDone, stats.crosswordsTotal))
        Text(stringResource(R.string.home_stat_essays, stats.essaysDone, stats.essaysTotal))
        Text(stringResource(R.string.home_stat_saved, stats.savedWords))
    }
}

@Composable
private fun ProgressStrip(
    score: Int,
    quizStreak: Int,
    dailyVisitStreak: Int,
    badges: List<String>,
    badgeScale: Float
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
            .background(scheme.surface.copy(alpha = 0.92f))
            .padding(AlbagramDimens.sectionGap)
    ) {
        Text(
            stringResource(R.string.home_progress),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.primary
        )
        Spacer(Modifier.height(AlbagramDimens.tightGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                icon = Icons.Default.Stars,
                label = stringResource(R.string.home_score),
                value = score.toString(),
                modifier = Modifier.weight(1f)
            )
            StatChip(
                icon = Icons.Default.LocalFireDepartment,
                label = stringResource(R.string.home_quiz_streak),
                value = quizStreak.toString(),
                modifier = Modifier.weight(1f)
            )
            StatChip(
                icon = Icons.Default.LocalFireDepartment,
                label = stringResource(R.string.home_daily_streak),
                value = dailyVisitStreak.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        if (badges.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.home_badges),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .scale(badgeScale),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { badge ->
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.secondaryContainer.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AlbagramDimens.chipRadius))
            .background(scheme.primary.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = scheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}
