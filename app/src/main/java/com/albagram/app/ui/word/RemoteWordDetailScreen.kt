package com.albagram.app.ui.word

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.albagram.app.R
import com.albagram.app.data.repository.BookRepository
import com.albagram.app.data.repository.WordRepository
import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import com.albagram.app.ui.components.AlbagramScaffold
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.components.SectionLabel
import com.albagram.app.ui.navigation.RemoteWordNav
import com.albagram.app.ui.theme.AlbagramDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class RemoteWordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val words: WordRepository,
    private val book: BookRepository,
    private val json: Json
) : ViewModel() {
    val entry: RemoteDictionaryEntry? = savedStateHandle.get<String>("payload")?.let {
        RemoteWordNav.decode(it, json)
    }
    private val savedWordId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val saved: StateFlow<Boolean> = savedWordId
        .flatMapLatest { id ->
            if (id == null) flowOf(false) else book.observeIsSaved(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            entry?.let { ensureLocalWord(it) }
        }
    }

    private suspend fun ensureLocalWord(entry: RemoteDictionaryEntry): String {
        val existing = savedWordId.value
        if (existing != null) return existing
        val wordId = words.upsertRemoteEntry(
            term = entry.term,
            definition = entry.definition,
            partOfSpeech = entry.partOfSpeech,
            sourceLabel = entry.sourceLabel,
            sourceUrl = entry.sourceUrl
        )
        savedWordId.value = wordId
        return wordId
    }

    fun toggleSave() {
        val currentEntry = entry ?: return
        viewModelScope.launch {
            val wordId = ensureLocalWord(currentEntry)
            if (saved.value) {
                book.remove(wordId)
            } else {
                book.save(wordId, "dictionary")
            }
        }
    }
}

@Composable
fun RemoteWordDetailScreen(
    onBack: () -> Unit,
    onHome: (() -> Unit)? = null,
    vm: RemoteWordDetailViewModel = hiltViewModel()
) {
    val entry = vm.entry
    val saved by vm.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AlbagramScaffold(
        title = entry?.term ?: stringResource(R.string.glossary_title),
        onBack = onBack,
        onHome = onHome
    ) { pad ->
        ScreenBackground {
            if (entry == null) {
                EmptyState(
                    text = stringResource(R.string.word_missing),
                    modifier = Modifier.padding(pad.values)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad.values)
                        .verticalScroll(rememberScrollState())
                        .padding(AlbagramDimens.screenPadding)
                ) {
                    Text(entry.term, style = MaterialTheme.typography.displayLarge)
                    entry.partOfSpeech?.let { SectionLabel(it) }
                    Spacer(Modifier.height(AlbagramDimens.tightGap))
                    SectionLabel(stringResource(R.string.word_definition))
                    Text(entry.definition, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(AlbagramDimens.sectionGap))
                    SectionLabel(stringResource(R.string.dictionary_source_label))
                    Text(
                        entry.sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse(entry.sourceUrl)
                            if (uri.scheme != "http" && uri.scheme != "https") {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.dictionary_open_source_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.dictionary_open_source_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dictionary_open_source))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = vm::toggleSave,
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
