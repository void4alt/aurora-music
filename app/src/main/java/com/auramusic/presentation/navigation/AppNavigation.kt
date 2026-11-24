package com.auramusic.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.SharedFlow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auramusic.R
import com.auramusic.models.PlayerUiState
import com.auramusic.player.PlayerViewModel
import com.auramusic.presentation.components.MiniPlayer
import com.auramusic.presentation.screens.ExploreScreen
import com.auramusic.presentation.screens.HomeScreen
import com.auramusic.presentation.screens.LibraryScreen
import com.auramusic.presentation.screens.PlayerScreen
import com.auramusic.presentation.screens.SearchScreen

private data class BottomItem(val route: String, val label: Int, val icon: ImageVector)

@Composable
fun AppNavigation(
    playerViewModel: PlayerViewModel,
    playerState: PlayerUiState,
    message: SharedFlow<String>
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        message.collect { snackbarHostState.showSnackbar(it) }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showChrome = currentDestination?.hierarchy?.none { it.route == AppRoute.PLAYER } ?: true

    val items = listOf(
        BottomItem(AppRoute.HOME, R.string.home, Icons.Filled.Home),
        BottomItem(AppRoute.EXPLORE, R.string.explore, Icons.Filled.Explore),
        BottomItem(AppRoute.SEARCH, R.string.search, Icons.Filled.Search),
        BottomItem(AppRoute.LIBRARY, R.string.library, Icons.Filled.LibraryMusic)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showChrome) {
                Column {
                    AnimatedVisibility(
                        visible = playerState.currentSong != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        MiniPlayer(
                            state = playerState,
                            onOpen = { navController.navigate(AppRoute.PLAYER) },
                            onTogglePlay = { playerViewModel.togglePlay() },
                            onNext = { playerViewModel.skipNext() }
                        )
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        items.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(
                route = AppRoute.HOME,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) }
            ) { HomeScreen(playerViewModel) }
            composable(
                route = AppRoute.EXPLORE,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) }
            ) { ExploreScreen(playerViewModel) }
            composable(
                route = AppRoute.SEARCH,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) }
            ) { SearchScreen(playerViewModel) }
            composable(
                route = AppRoute.LIBRARY,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) }
            ) { LibraryScreen(playerViewModel) }
            composable(
                route = AppRoute.PLAYER,
                enterTransition = {
                    slideInVertically(animationSpec = tween(320)) { it / 3 } + fadeIn(tween(240))
                },
                exitTransition = {
                    slideOutVertically(animationSpec = tween(260)) { it / 3 } + fadeOut(tween(200))
                }
            ) {
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
