package com.albagram.app.ui.libri

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.local.entity.SavedWordListItem
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.data.repository.BookRepository
import com.albagram.app.data.repository.DailyChallengeRepository
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.domain.progress.ProgressMilestones
import com.albagram.app.domain.quiz.AdvanceLock
import com.albagram.app.ui.components.AlbagramHaptics
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.ModuleScreenHeader
import com.albagram.app.ui.components.SaveBookmarkButton
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SessionSummaryCard
import com.albagram.app.ui.components.WordListItem
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.theme.AlbagramDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibriViewModel @Inject constructor(
    private val book: BookRepository,
    private val wordsRepo: WordRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    var weeklyWords by mutableStateOf(emptyList<WordEntity>())
        private set

    var reviewing by mutableStateOf(false)
        private set
    var reviewCards by mutableStateOf<List<SavedWordListItem>>(emptyList())
        private set
    var reviewIndex by mutableIntStateOf(0)
        private set
    var revealTerm by mutableStateOf(false)
        private set
    var reviewPoints by mutableIntStateOf(0)
        private set
    var reviewFinished by mutableStateOf(false)
        private set
    private val advanceLock = AdvanceLock()

    fun setQuery(value: String) { query.value = value }

    @OptIn(ExperimentalCoroutinesApi::class)
    val words = query
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { book.observeSaved(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val count = book.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val searchQuery = query.asStateFlow()

    init {
        viewModelScope.launch {
            val daily = dailyChallengeRepository.forDate()
            weeklyWords = wordsRepo.getByIds(daily.weeklyWordIds)
        }
    }

    fun remove(wordId: String) {
        viewModelScope.launch { book.remove(wordId) }
    }

    fun startReview(saved: List<SavedWordListItem>) {
        if (saved.size < 3) return
        reviewCards = saved.shuffled()
        reviewIndex = 0
        revealTerm = false
        reviewPoints = 0
        reviewFinished = false
        reviewing = true
        advanceLock.unlock()
    }

    fun reveal() {
        revealTerm = true
    }

    fun rateKnown(known: Boolean) {
        if (!advanceLock.tryLock()) return
        val card = reviewCards.getOrNull(reviewIndex) ?: run {
            advanceLock.unlock()
            return
        }
        viewModelScope.launch {
            try {
                if (known) {
                    reviewPoints += 3
                    progressRepository.addScore(3)
                    book.markReviewed(card.word.wordId)
                }
                if (reviewIndex >= reviewCards.lastIndex) {
                    reviewing = false
                    reviewFinished = true
                    if (reviewPoints >= 9) {
                        progressRepository.awardBadge(ProgressMilestones.BADGE_LEXUES)
                    }
                } else {
                    reviewIndex += 1
                    revealTerm = false
                }
            } finally {
                advanceLock.unlock()
            }
        }
    }

    fun dismissReviewSummary() {
        reviewFinished = false
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibriScreen(
    contentPadding: PaddingValues,
    onOpenWord: (String) -> Unit,
    onOpenGlossary: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    vm: LibriViewModel = hiltViewModel()
) {
    val words by vm.words.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val weeklyWords = vm.weeklyWords

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(AlbagramDimens.screenPadding)
        ) {
            ModuleScreenHeader(
                title = stringResource(R.string.libri_title, count),
                onHome = onHome,
                actions = {
                    if (onOpenGlossary != null) {
                        TextButton(onClick = onOpenGlossary) {
                            Text(stringResource(R.string.glossary_action))
                        }
                    }
                }
            )

            if (vm.reviewing) {
                ReviewSession(vm)
                return@Column
            }

            if (vm.reviewFinished) {
                SessionSummaryCard(
                    title = stringResource(R.string.libri_review_done),
                    lines = listOf(stringResource(R.string.libri_review_points, vm.reviewPoints))
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::dismissReviewSummary) {
                    Text(stringResource(R.string.badge_celebration_dismiss))
                }
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AlbagramDimens.tightGap)
                    .imePadding(),
                placeholder = { Text(stringResource(R.string.libri_search)) },
                singleLine = true
            )

            if (count >= 3 && query.isBlank()) {
                Button(
                    onClick = { vm.startReview(words) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.libri_review_start))
                }
                Spacer(Modifier.height(8.dp))
            }

            if (query.isBlank()) {
                SessionSummaryCard(
                    title = stringResource(R.string.libri_weekly_title),
                    lines = listOf(stringResource(R.string.libri_weekly_subtitle))
                )
                if (weeklyWords.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weeklyWords.forEach { word ->
                            AssistChip(
                                onClick = { onOpenWord(word.wordId) },
                                label = { Text(word.term) }
                            )
                        }
                    }
                }
            }
            if (words.isEmpty()) {
                EmptyState(
                    if (query.isBlank()) {
                        stringResource(R.string.libri_empty)
                    } else {
                        stringResource(R.string.libri_no_hits)
                    },
                    actionLabel = if (query.isBlank() && onOpenGlossary != null) {
                        stringResource(R.string.libri_open_glossary)
                    } else {
                        null
                    },
                    onAction = onOpenGlossary
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(words, key = { it.word.wordId }) { item ->
                        WordListItem(
                            word = item.word,
                            onClick = { onOpenWord(item.word.wordId) },
                            footnote = stringResource(R.string.libri_reviewed, item.timesReviewed),
                            trailing = {
                                SaveBookmarkButton(saved = true) { vm.remove(item.word.wordId) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSession(vm: LibriViewModel) {
    val card = vm.reviewCards.getOrNull(vm.reviewIndex) ?: return
    Text(
        stringResource(R.string.libri_review_progress, vm.reviewIndex + 1, vm.reviewCards.size),
        style = MaterialTheme.typography.labelLarge
    )
    Spacer(Modifier.height(12.dp))
    Text(card.word.definition, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    if (vm.revealTerm) {
        Text(
            card.word.term,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        RowActions(vm)
    } else {
        Button(onClick = vm::reveal, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.libri_review_reveal))
        }
    }
}

@Composable
private fun RowActions(vm: LibriViewModel) {
    val hapticView = rememberHapticView()
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                AlbagramHaptics.light(hapticView)
                vm.rateKnown(false)
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.libri_review_again))
        }
        Button(
            onClick = {
                AlbagramHaptics.success(hapticView)
                vm.rateKnown(true)
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.libri_review_know))
        }
    }
}
