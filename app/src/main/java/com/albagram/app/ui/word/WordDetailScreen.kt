package com.albagram.app.ui.word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.data.repository.BookRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.ui.components.AlbagramScaffold
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.LoadingState
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SectionLabel
import com.albagram.app.ui.theme.AlbagramDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordDetailUi(
    val loading: Boolean = true,
    val word: WordEntity? = null,
    val synonyms: List<WordEntity> = emptyList(),
    val antonyms: List<WordEntity> = emptyList(),
    val missing: Boolean = false
)

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val words: WordRepository,
    private val book: BookRepository
) : ViewModel() {
    private val wordId: String = checkNotNull(savedStateHandle["wordId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui = words.observeById(wordId)
        .mapLatest { word ->
            if (word == null) {
                WordDetailUi(loading = false, missing = true)
            } else {
                WordDetailUi(
                    loading = false,
                    word = word,
                    synonyms = words.getByIds(words.parseIds(word.synonymsJson)),
                    antonyms = words.getByIds(words.parseIds(word.antonymsJson)),
                    missing = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordDetailUi())

    val saved = book.observeIsSaved(wordId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { book.markReviewed(wordId) }
    }

    fun toggleSave(source: String = "manual") {
        viewModelScope.launch {
            if (saved.value) book.remove(wordId) else book.save(wordId, source)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDetailScreen(
    onBack: () -> Unit,
    onHome: (() -> Unit)? = null,
    onOpenWord: (String) -> Unit = {},
    vm: WordDetailViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val word = ui.word

    AlbagramScaffold(
        title = word?.term ?: stringResource(R.string.glossary_title),
        onBack = onBack,
        onHome = onHome
    ) { pad ->
        ScreenBackground {
            when {
                ui.loading -> LoadingState(modifier = Modifier.padding(pad.values))
                ui.missing || word == null -> EmptyState(
                    stringResource(R.string.word_missing),
                    modifier = Modifier.padding(pad.values)
                )
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad.values)
                            .verticalScroll(rememberScrollState())
                            .padding(AlbagramDimens.screenPadding)
                    ) {
                        Text(word.term, style = MaterialTheme.typography.displayLarge)
                        word.partOfSpeech?.let { SectionLabel(it) }
                        Spacer(Modifier.height(AlbagramDimens.tightGap))
                        SectionLabel(stringResource(R.string.word_definition))
                        Text(word.definition, style = MaterialTheme.typography.bodyLarge)
                        word.exampleSentence?.let {
                            Spacer(Modifier.height(AlbagramDimens.sectionGap))
                            SectionLabel(stringResource(R.string.word_example))
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (ui.synonyms.isNotEmpty()) {
                            Spacer(Modifier.height(AlbagramDimens.sectionGap))
                            SectionLabel(stringResource(R.string.word_synonyms))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ui.synonyms.forEach { related ->
                                    AssistChip(
                                        onClick = { onOpenWord(related.wordId) },
                                        label = { Text(related.term) }
                                    )
                                }
                            }
                        }
                        if (ui.antonyms.isNotEmpty()) {
                            Spacer(Modifier.height(AlbagramDimens.sectionGap))
                            SectionLabel(stringResource(R.string.word_antonyms))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ui.antonyms.forEach { related ->
                                    AssistChip(
                                        onClick = { onOpenWord(related.wordId) },
                                        label = { Text(related.term) }
                                    )
                                }
                            }
                        }
                        word.usageNote?.let {
                            Spacer(Modifier.height(AlbagramDimens.sectionGap))
                            SectionLabel(stringResource(R.string.word_usage))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { vm.toggleSave() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(
                                    if (saved) R.string.word_unsave else R.string.word_save
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
