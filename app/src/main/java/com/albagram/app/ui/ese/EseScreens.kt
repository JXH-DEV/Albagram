package com.albagram.app.ui.ese

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.local.entity.EssayPromptEntity
import com.albagram.app.data.local.entity.WordEntity
import androidx.compose.material3.Button
import com.albagram.app.data.repository.EssayRepository
import com.albagram.app.data.repository.DailyChallengeRepository
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.domain.daily.DailyChallengeKeys
import com.albagram.app.domain.ese.EseCompletionRules
import com.albagram.app.domain.progress.ProgressMilestones
import kotlinx.coroutines.flow.combine
import com.albagram.app.ui.components.AlbagramHaptics
import com.albagram.app.ui.components.AlbagramScaffold
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.LoadingState
import com.albagram.app.ui.components.ModuleCard
import com.albagram.app.ui.components.ModuleContentState
import com.albagram.app.ui.components.ModuleScreenHeader
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SessionSummaryCard
import com.albagram.app.ui.components.SectionLabel
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.theme.AlbagramDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EseListViewModel @Inject constructor(
    essayRepository: EssayRepository
) : ViewModel() {
    private val loaded = MutableStateFlow(false)
    val prompts = essayRepository.observePrompts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val drafts = essayRepository.observeAllDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val promptRows = combine(prompts, drafts) { promptList, draftList ->
        val draftMap = draftList.associateBy { it.promptId }
        promptList.map { prompt ->
            val draft = draftMap[prompt.promptId]
            PromptRow(
                prompt = prompt,
                status = when {
                    draft?.completedAt != null -> PromptStatus.COMPLETED
                    draft != null && draft.body.isNotBlank() -> PromptStatus.DRAFT
                    else -> PromptStatus.NEW
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            prompts.collect { loaded.value = true }
        }
    }

    val isLoaded = loaded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

enum class PromptStatus { NEW, DRAFT, COMPLETED }

data class PromptRow(val prompt: EssayPromptEntity, val status: PromptStatus)

@Composable
fun EseListScreen(
    contentPadding: PaddingValues,
    onOpenPrompt: (String) -> Unit,
    onHome: (() -> Unit)? = null,
    vm: EseListViewModel = hiltViewModel()
) {
    val prompts by vm.promptRows.collectAsStateWithLifecycle()
    val loaded by vm.isLoaded.collectAsStateWithLifecycle()
    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(AlbagramDimens.screenPadding)
        ) {
            ModuleScreenHeader(
                title = stringResource(R.string.ese_title),
                subtitle = stringResource(R.string.ese_subtitle),
                onHome = onHome
            )
            Spacer(Modifier.height(AlbagramDimens.sectionGap))
            ModuleContentState(
                loading = !loaded,
                empty = prompts.isEmpty(),
                emptyText = stringResource(R.string.ese_empty)
            ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AlbagramDimens.itemGap)
                    ) {
                        items(prompts, key = { it.prompt.promptId }) { row ->
                            val badge = when (row.status) {
                                PromptStatus.COMPLETED -> stringResource(R.string.ese_status_completed)
                                PromptStatus.DRAFT -> stringResource(R.string.ese_status_draft)
                                PromptStatus.NEW -> null
                            }
                            ModuleCard(
                                title = row.prompt.title,
                                subtitle = buildString {
                                    badge?.let { append("[$it] ") }
                                    append(row.prompt.promptText.take(120))
                                },
                                icon = Icons.Default.EditNote,
                                onClick = { onOpenPrompt(row.prompt.promptId) }
                            )
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class EseWriteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val essayRepository: EssayRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {
    private val promptId: String = checkNotNull(savedStateHandle["promptId"])
    private var saveJob: Job? = null

    var prompt by mutableStateOf<EssayPromptEntity?>(null)
        private set
    var suggested by mutableStateOf<List<WordEntity>>(emptyList())
        private set
    var draft by mutableStateOf("")
        private set
    var loading by mutableStateOf(true)
        private set
    var hydrated by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var submitted by mutableStateOf(false)
        private set
    var submitSummary by mutableStateOf<List<String>>(emptyList())
        private set
    var completedAt by mutableStateOf<Long?>(null)
        private set

    val canSubmit: Boolean
        get() = EseCompletionRules.canSubmit(draft, suggested.map { it.term }) && completedAt == null

    init {
        viewModelScope.launch {
            try {
                val p = essayRepository.getPrompt(promptId)
                prompt = p
                if (p != null) {
                    suggested = essayRepository.suggestedWords(p)
                }
                val existing = essayRepository.getDraft(promptId)
                if (existing != null) {
                    draft = existing.body
                    completedAt = existing.completedAt
                    submitted = existing.completedAt != null
                }
            } catch (_: Exception) {
                loadError = "Ngarkimi i temës dështoi. Provo përsëri."
            }
            hydrated = true
            loading = false
        }
    }

    private var pendingDraft: String? = null

    fun updateDraft(value: String) {
        if (!hydrated || completedAt != null) return
        draft = value
        pendingDraft = value
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            essayRepository.saveDraft(promptId, value)
            pendingDraft = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        pendingDraft?.let { toFlush ->
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                essayRepository.saveDraft(promptId, toFlush)
            }
        }
    }

    fun appendWord(term: String) {
        if (!hydrated || completedAt != null) return
        val addition = if (draft.isBlank() || draft.endsWith(" ") || draft.endsWith("\n")) term else " $term"
        updateDraft(draft + addition)
    }

    fun submit() {
        if (!canSubmit) return
        saveJob?.cancel()
        pendingDraft = null
        viewModelScope.launch {
            essayRepository.submitEssay(promptId, draft)
            completedAt = System.currentTimeMillis()
            submitted = true
            val used = EseCompletionRules.usedSuggestedCount(draft, suggested.map { it.term })
            val words = EseCompletionRules.wordCount(draft)
            submitSummary = listOf(
                "Fjalë: $words",
                "Fjalë të sugjeruara: $used/${suggested.size}"
            )
            progressRepository.addScore(15)
            progressRepository.awardBadge(ProgressMilestones.BADGE_SHKRIMTAR)
            val daily = dailyChallengeRepository.forDate()
            if (daily.essayPromptId == promptId) {
                progressRepository.markDailyComplete(DailyChallengeKeys.ESE)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EseWriteScreen(
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onHome: (() -> Unit)? = null,
    vm: EseWriteViewModel = hiltViewModel()
) {
    val prompt = vm.prompt
    val hapticView = rememberHapticView()
    LaunchedEffect(vm.submitted) {
        if (vm.submitted) AlbagramHaptics.success(hapticView)
    }
    AlbagramScaffold(
        title = prompt?.title ?: stringResource(R.string.ese_title),
        onBack = onBack,
        onHome = onHome
    ) { pad ->
        ScreenBackground {
            when {
                vm.loading -> LoadingState()
                prompt == null -> EmptyState(stringResource(R.string.ese_empty))
                else -> {
                    val wordCount = EseCompletionRules.wordCount(vm.draft)
                    val suggestedUsed = EseCompletionRules.usedSuggestedCount(
                        vm.draft,
                        vm.suggested.map { it.term }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad.values)
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(AlbagramDimens.screenPadding)
                    ) {
                        vm.loadError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(AlbagramDimens.tightGap))
                        }
                        Text(prompt.promptText, style = MaterialTheme.typography.bodyLarge)
                        SessionSummaryCard(
                            title = stringResource(R.string.session_summary_title),
                            lines = listOf(
                                stringResource(R.string.ese_summary_words, wordCount),
                                stringResource(
                                    R.string.ese_summary_suggested_used,
                                    suggestedUsed,
                                    vm.suggested.size
                                )
                            )
                        )
                        SectionLabel(stringResource(R.string.ese_suggested))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            vm.suggested.forEach { word ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AssistChip(
                                        onClick = { vm.appendWord(word.term) },
                                        label = { Text(word.term) }
                                    )
                                    IconButton(onClick = { onOpenWord(word.wordId) }) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = stringResource(R.string.ese_view_word)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(AlbagramDimens.sectionGap))
                        SectionLabel(stringResource(R.string.ese_draft))
                        OutlinedTextField(
                            value = vm.draft,
                            onValueChange = vm::updateDraft,
                            enabled = vm.hydrated && vm.completedAt == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                            placeholder = { Text(stringResource(R.string.ese_draft)) }
                        )
                        if (vm.submitted) {
                            Spacer(Modifier.height(AlbagramDimens.sectionGap))
                            SessionSummaryCard(
                                title = stringResource(R.string.ese_submit_done),
                                lines = vm.submitSummary
                            )
                        } else {
                            Spacer(Modifier.height(AlbagramDimens.sectionGap))
                            Button(
                                onClick = vm::submit,
                                enabled = vm.canSubmit,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.ese_submit))
                            }
                            if (!vm.canSubmit) {
                                Text(
                                    stringResource(R.string.ese_submit_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
