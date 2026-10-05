package com.albagram.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.albagram.app.R
import com.albagram.app.data.seed.SeedRepository
import com.albagram.app.data.seed.SeedState
import com.albagram.app.domain.dictionary.RemoteDictionaryEntry
import com.albagram.app.ui.components.EmptyState
import com.albagram.app.ui.components.LoadingState
import com.albagram.app.ui.components.ScreenBackground
import com.albagram.app.ui.crossword.CrosswordListScreen
import com.albagram.app.ui.crossword.CrosswordPlayScreen
import com.albagram.app.ui.ese.EseListScreen
import com.albagram.app.ui.ese.EseWriteScreen
import com.albagram.app.ui.glossary.GlossaryScreen
import com.albagram.app.ui.home.HomeScreen
import com.albagram.app.ui.konkursi.KonkursiScreen
import com.albagram.app.ui.libri.LibriScreen
import com.albagram.app.ui.rima.RimaScreen
import com.albagram.app.ui.word.RemoteWordDetailScreen
import com.albagram.app.ui.word.WordDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

private data class TabSpec(
    val dest: BottomDest,
    val icon: ImageVector,
    val labelRes: Int
)

private val tabs = listOf(
    TabSpec(BottomDest.Crossword, Icons.Default.Extension, R.string.nav_crossword),
    TabSpec(BottomDest.Rima, Icons.Default.MusicNote, R.string.nav_rima),
    TabSpec(BottomDest.Ese, Icons.Default.EditNote, R.string.nav_ese),
    TabSpec(BottomDest.Konkursi, Icons.Default.EmojiEvents, R.string.nav_konkursi),
    TabSpec(BottomDest.Libri, Icons.Default.AutoStories, R.string.nav_libri)
)

@HiltViewModel
class SeedGateViewModel @Inject constructor(
    private val seedRepository: SeedRepository
) : ViewModel() {
    val seedState = seedRepository.state

    fun retry() {
        viewModelScope.launch { seedRepository.retry() }
    }
}

@Composable
fun AlbagramNavHost(
    seedVm: SeedGateViewModel = hiltViewModel()
) {
    val seedState by seedVm.seedState.collectAsStateWithLifecycle()

    when (seedState) {
        is SeedState.Loading -> {
            ScreenBackground { LoadingState() }
        }
        is SeedState.Failed -> {
            val failed = seedState as SeedState.Failed
            ScreenBackground {
                EmptyState(
                    text = failed.message.ifBlank { stringResource(R.string.seed_failed) },
                    actionLabel = stringResource(R.string.retry),
                    onAction = seedVm::retry
                )
            }
        }
        is SeedState.Ready -> {
            AlbagramReadyNav()
        }
    }
}

@Composable
private fun AlbagramReadyNav() {
    val navController = rememberNavController()
    val navJson = remember { Json { ignoreUnknownKeys = true; isLenient = true } }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val tabRoutes = tabs.map { it.dest.route }
    val showBottomBar = currentRoute in tabRoutes

    fun openWord(wordId: String) {
        navController.navigate(Routes.WordDetail.create(wordId))
    }

    fun openRemoteWord(entry: RemoteDictionaryEntry) {
        navController.navigate(
            Routes.RemoteWordDetail.create(RemoteWordNav.encode(entry, navJson))
        )
    }

    fun goHome() {
        navController.navigate(Routes.Home.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.dest.route,
                            onClick = { openTab(tab.dest.navRoute) },
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home.route) {
                HomeScreen(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onOpenModule = ::openTab,
                    onOpenGlossary = { navController.navigate(Routes.Glossary.route) },
                    onOpenDailyCrossword = { id ->
                        navController.navigate(Routes.CrosswordPlay.create(id))
                    },
                    onOpenDailyQuiz = {
                        navController.navigate(Routes.Konkursi.create(daily = true)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenDailyWord = ::openWord,
                    onResumeCrossword = { id ->
                        navController.navigate(Routes.CrosswordPlay.create(id))
                    },
                    onResumeEssay = { id ->
                        navController.navigate(Routes.EseWrite.create(id))
                    }
                )
            }
            composable(Routes.Crossword.route) {
                CrosswordListScreen(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onOpenPuzzle = { id -> navController.navigate(Routes.CrosswordPlay.create(id)) },
                    onHome = ::goHome
                )
            }
            composable(
                route = "crossword/{puzzleId}",
                arguments = listOf(navArgument("puzzleId") { type = NavType.StringType })
            ) {
                CrosswordPlayScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWord = ::openWord,
                    onHome = ::goHome,
                    onOpenDailyQuiz = {
                        navController.navigate(Routes.Konkursi.create(daily = true)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.Rima.route) {
                RimaScreen(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onOpenWord = ::openWord,
                    onHome = ::goHome
                )
            }
            composable(Routes.Ese.route) {
                EseListScreen(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onOpenPrompt = { id -> navController.navigate(Routes.EseWrite.create(id)) },
                    onHome = ::goHome
                )
            }
            composable(
                route = "ese/{promptId}",
                arguments = listOf(navArgument("promptId") { type = NavType.StringType })
            ) {
                EseWriteScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWord = ::openWord,
                    onHome = ::goHome
                )
            }
            composable(
                route = Routes.Konkursi.route,
                arguments = listOf(
                    navArgument("daily") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) {
                KonkursiScreen(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onHome = ::goHome,
                    onOpenDailyQuiz = {
                        navController.navigate(Routes.Konkursi.create(daily = true)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.Libri.route) {
                LibriScreen(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onOpenWord = ::openWord,
                    onOpenGlossary = { navController.navigate(Routes.Glossary.route) },
                    onHome = ::goHome
                )
            }
            composable(Routes.Glossary.route) {
                GlossaryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWord = ::openWord,
                    onOpenRemoteWord = ::openRemoteWord,
                    onHome = ::goHome
                )
            }
            composable(
                route = Routes.RemoteWordDetail.route,
                arguments = listOf(navArgument("payload") { type = NavType.StringType })
            ) {
                RemoteWordDetailScreen(
                    onBack = { navController.popBackStack() },
                    onHome = ::goHome
                )
            }
            composable(
                route = "word/{wordId}",
                arguments = listOf(navArgument("wordId") { type = NavType.StringType })
            ) {
                WordDetailScreen(
                    onBack = { navController.popBackStack() },
                    onHome = ::goHome,
                    onOpenWord = ::openWord
                )
            }
        }
    }
}
