package com.albagram.app.ui.glossary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.albagram.app.data.local.entity.UserProgressEntity
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.ui.components.SessionSummaryCard
import com.albagram.app.ui.theme.Leaf
import com.albagram.app.ui.theme.Coral
import com.albagram.app.data.remote.DictionaryRemoteRepository
import com.albagram.app.data.remote.OfflineException
import com.albagram.app.data.repository.DailyChallengeRepository
import com.albagram.app.data.repository.ProgressRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.domain.daily.DailyChallengeKeys
import kotlin.random.Random
import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import com.albagram.app.domain.dictionary.RemoteSearchState
import com.albagram.app.domain.rhyme.RhymeMatcher
import com.albagram.app.ui.components.AlbagramScaffold
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.LoadingState
import com.albagram.app.ui.components.AlbagramHaptics
import com.albagram.app.ui.components.RemoteWordListItem
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SectionLabel
import com.albagram.app.ui.components.WordListItem
import com.albagram.app.ui.components.rememberHapticView
import com.albagram.app.ui.theme.AlbagramDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val ALPHABET = listOf(
    "Të gjitha",
    "A", "B", "C", "Ç", "D", "E", "F", "G", "H", "I", "J", "K", "L",
    "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "X", "Y", "Z"
)

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val words: WordRepository,
    private val remoteDictionary: DictionaryRemoteRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    private val letter = MutableStateFlow("Të gjitha")
    private val query = MutableStateFlow("")
    private val loaded = MutableStateFlow(false)
    private val remoteState = MutableStateFlow(RemoteSearchState())
    private var remoteJob: Job? = null

    fun setLetter(value: String) { letter.value = value }
    fun setQuery(value: String) {
        query.value = value
        scheduleRemoteSearch(value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val items = query.flatMapLatest { q ->
        if (q.isNotBlank()) {
            words.search(q.trim())
        } else {
            letter.flatMapLatest { l ->
                if (l == "Të gjitha") words.observeGlossary()
                else words.observeByLetter(l)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val remoteSearch = remoteState.asStateFlow()
    val isLoaded = loaded.asStateFlow()
    val selectedLetter = letter.asStateFlow()
    val searchQuery = query.asStateFlow()

    var dailyWord by mutableStateOf<WordEntity?>(null)
        private set
    var dailyOptions by mutableStateOf<List<String>>(emptyList())
        private set
    var dailyComplete by mutableStateOf(false)
        private set
    var dailyAnswered by mutableStateOf<Boolean?>(null)
        private set
    var dailyLoaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            items.collect { loaded.value = true }
        }
        viewModelScope.launch {
            loadDailyChallenge()
            progressRepository.observe().collect { entity ->
                val p = entity ?: UserProgressEntity()
                dailyComplete = DailyChallengeKeys.WORD in progressRepository.dailyCompletedKeys(p)
            }
        }
    }

    private suspend fun loadDailyChallenge() {
        val daily = dailyChallengeRepository.forDate()
        val wordId = daily.wordId ?: return
        val word = words.getById(wordId) ?: return
        dailyWord = word
        val corpus = words.getAllGlossary().filter { it.wordId != wordId }
        val distractors = corpus.shuffled(Random.Default).take(2).map { it.definition }
        dailyOptions = (distractors + word.definition).shuffled(Random.Default)
        dailyLoaded = true
    }

    fun answerDaily(option: String) {
        val word = dailyWord ?: return
        if (dailyAnswered != null || dailyComplete) return
        val correct = option == word.definition
        dailyAnswered = correct
        viewModelScope.launch {
            if (correct) {
                progressRepository.addScore(5)
                progressRepository.markDailyComplete(DailyChallengeKeys.WORD)
                dailyComplete = true
            }
        }
    }

    private fun scheduleRemoteSearch(rawQuery: String) {
        remoteJob?.cancel()
        val q = rawQuery.trim()
        if (q.isBlank()) {
            remoteState.value = RemoteSearchState()
            return
        }

        remoteJob = viewModelScope.launch {
            delay(400)
            if (!remoteDictionary.isOnline()) {
                remoteState.value = RemoteSearchState(offline = true)
                return@launch
            }

            remoteState.value = RemoteSearchState(loading = true)
            remoteDictionary.search(q)
                .onSuccess { results ->
                    val localTerms = items.value
                        .map { RhymeMatcher.normalize(it.term) }
                        .toSet()
                    remoteState.value = RemoteSearchState(
                        loading = false,
                        results = results.filter {
                            RhymeMatcher.normalize(it.term) !in localTerms
                        }
                    )
                }
                .onFailure { error ->
                    remoteState.value = RemoteSearchState(
                        loading = false,
                        offline = error is OfflineException,
                        error = if (error is OfflineException) null else error.message
                    )
                }
        }
    }
}

@Composable
fun GlossaryScreen(
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onOpenRemoteWord: (RemoteDictionaryEntry) -> Unit,
    onHome: (() -> Unit)? = null,
    vm: GlossaryViewModel = hiltViewModel()
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val selected by vm.selectedLetter.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val loaded by vm.isLoaded.collectAsStateWithLifecycle()
    val remote by vm.remoteSearch.collectAsStateWithLifecycle()
    val hasQuery = query.isNotBlank()
    val showEmpty = loaded &&
        items.isEmpty() &&
        remote.results.isEmpty() &&
        !remote.loading &&
        hasQuery

    AlbagramScaffold(
        title = stringResource(R.string.glossary_title),
        onBack = onBack,
        onHome = onHome
    ) { pad ->
        ScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad.values)
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AlbagramDimens.screenPadding),
                    placeholder = { Text(stringResource(R.string.glossary_search)) },
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    ALPHABET.forEach { letter ->
                        FilterChip(
                            selected = selected == letter && query.isBlank(),
                            onClick = {
                                vm.setQuery("")
                                vm.setLetter(letter)
                            },
                            label = { Text(letter) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                if (query.isBlank() && vm.dailyLoaded && vm.dailyWord != null && !vm.dailyComplete) {
                    DailyWordQuizBanner(vm)
                }
                when {
                    !loaded -> LoadingState()
                    showEmpty -> EmptyState(stringResource(R.string.glossary_no_hits))
                    items.isEmpty() && !hasQuery -> EmptyState(stringResource(R.string.glossary_letter_empty))
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(AlbagramDimens.screenPadding),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (items.isNotEmpty()) {
                                items(items, key = { it.wordId }) { word ->
                                    WordListItem(word = word, onClick = { onOpenWord(word.wordId) })
                                }
                            }

                            if (hasQuery) {
                                item(key = "remote-header") {
                                    RemoteSearchHeader(remote = remote)
                                }
                                if (remote.loading && remote.results.isEmpty()) {
                                    item(key = "remote-loading") {
                                        LoadingState(text = stringResource(R.string.dictionary_online_loading))
                                    }
                                }
                                items(remote.results, key = { "${it.sourceId}:${it.term}" }) { entry ->
                                    RemoteWordListItem(
                                        entry = entry,
                                        onClick = { onOpenRemoteWord(entry) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyWordQuizBanner(vm: GlossaryViewModel) {
    val word = vm.dailyWord ?: return
    val hapticView = rememberHapticView()
    LaunchedEffect(vm.dailyAnswered) {
        when (vm.dailyAnswered) {
            true -> AlbagramHaptics.success(hapticView)
            false -> AlbagramHaptics.error(hapticView)
            null -> Unit
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AlbagramDimens.screenPadding, vertical = 8.dp)
    ) {
        SessionSummaryCard(
            title = stringResource(R.string.glossary_daily_title),
            lines = listOf(
                stringResource(R.string.glossary_daily_prompt, word.term),
                stringResource(R.string.glossary_daily_question)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        vm.dailyOptions.forEach { option ->
            OutlinedButton(
                onClick = { vm.answerDaily(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                enabled = vm.dailyAnswered == null
            ) {
                Text(option)
            }
        }
        vm.dailyAnswered?.let { ok ->
            Text(
                if (ok) stringResource(R.string.konkursi_correct) else stringResource(R.string.konkursi_wrong),
                color = if (ok) Leaf else Coral,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RemoteSearchHeader(remote: RemoteSearchState) {
    Column(modifier = Modifier.padding(vertical = AlbagramDimens.tightGap)) {
        SectionLabel(stringResource(R.string.dictionary_online_section))
        when {
            remote.offline -> Text(
                text = stringResource(R.string.dictionary_offline_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AlbagramDimens.tightGap)
            )
            remote.error != null -> Text(
                text = stringResource(R.string.dictionary_lookup_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = AlbagramDimens.tightGap)
            )
        }
    }
}
