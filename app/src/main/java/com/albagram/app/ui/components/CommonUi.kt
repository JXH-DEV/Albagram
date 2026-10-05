package com.albagram.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.albagram.app.R
import com.albagram.app.data.local.entity.WordEntity
import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import com.albagram.app.ui.theme.AlbagramDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbagramScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    topBarActions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValuesApp) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    } else if (onHome != null) {
                        IconButton(onClick = onHome) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = stringResource(R.string.cd_home)
                            )
                        }
                    }
                },
                actions = {
                    if (onBack != null && onHome != null) {
                        IconButton(onClick = onHome) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = stringResource(R.string.cd_home)
                            )
                        }
                    }
                    topBarActions()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        content(PaddingValuesApp(padding))
    }
}

data class PaddingValuesApp(val values: androidx.compose.foundation.layout.PaddingValues)

@Composable
fun ScreenBackground(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.background,
                        scheme.surfaceVariant.copy(alpha = 0.45f),
                        scheme.background
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val label = "$title. $subtitle"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
            .background(
                if (emphasized) scheme.primaryContainer.copy(alpha = 0.85f)
                else scheme.surface.copy(alpha = 0.92f)
            )
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .clickable(onClick = onClick)
            .padding(AlbagramDimens.sectionGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(AlbagramDimens.iconBox)
                .clip(RoundedCornerShape(AlbagramDimens.chipRadius))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WordListItem(
    word: WordEntity,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    footnote: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
                .background(scheme.surface.copy(alpha = 0.92f))
                .clickable(onClick = onClick)
                .padding(AlbagramDimens.sectionGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AlbagramDimens.letterAvatar)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    word.letterOfAlphabet,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(word.term, style = MaterialTheme.typography.titleLarge)
                Text(
                    word.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2
                )
                if (footnote != null) {
                    Text(
                        footnote,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary
                    )
                }
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(AlbagramDimens.tightGap))
    }
}

@Composable
fun RemoteWordListItem(
    entry: RemoteDictionaryEntry,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val letter = entry.term.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val sourceLabel = when (entry.sourceId) {
        "fjalori.online" -> stringResource(R.string.dictionary_source_fjalori_online)
        "fjalori.shkenca.org" -> stringResource(R.string.dictionary_source_shkenca)
        else -> entry.sourceLabel
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AlbagramDimens.cardRadius))
                .background(scheme.secondaryContainer.copy(alpha = 0.55f))
                .clickable(onClick = onClick)
                .padding(AlbagramDimens.sectionGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AlbagramDimens.letterAvatar)
                    .clip(CircleShape)
                    .background(scheme.secondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letter,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(entry.term, style = MaterialTheme.typography.titleLarge)
                Text(
                    entry.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2
                )
                Text(
                    sourceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.secondary
                )
            }
        }
        Spacer(Modifier.height(AlbagramDimens.tightGap))
    }
}

@Composable
fun SaveBookmarkButton(saved: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = stringResource(
                if (saved) R.string.cd_remove_bookmark else R.string.cd_add_bookmark
            ),
            tint = if (saved) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = AlbagramDimens.tightGap)
    )
}

@Composable
fun ModuleScreenHeader(
    title: String,
    subtitle: String? = null,
    onHome: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onHome != null) {
            IconButton(onClick = onHome) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = stringResource(R.string.cd_home)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        actions()
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.Inbox
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(AlbagramDimens.sectionGap))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.loading)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(AlbagramDimens.sectionGap))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModuleContentState(
    loading: Boolean,
    empty: Boolean,
    emptyText: String,
    loadingText: String = stringResource(R.string.loading),
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    when {
        loading -> LoadingState(text = loadingText)
        empty -> EmptyState(
            text = emptyText,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction
        )
        else -> content()
    }
}

@Composable
fun SessionSummaryCard(
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AlbagramDimens.tightGap),
        shape = RoundedCornerShape(AlbagramDimens.cardRadius),
        color = scheme.surface.copy(alpha = 0.94f)
    ) {
        Column(modifier = Modifier.padding(AlbagramDimens.sectionGap)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            lines.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
