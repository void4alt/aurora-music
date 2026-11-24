package com.auramusic.presentation.navigation

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
fun AppNavigation(playerViewModel: PlayerViewModel, playerState: PlayerUiState) {
    val navController = rememberNavController()
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
        bottomBar = {
            if (showChrome) {
                Column {
                    if (playerState.currentSong != null) {
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
            composable(AppRoute.HOME) { HomeScreen(playerViewModel) }
            composable(AppRoute.EXPLORE) { ExploreScreen(playerViewModel) }
            composable(AppRoute.SEARCH) { SearchScreen(playerViewModel) }
            composable(AppRoute.LIBRARY) { LibraryScreen(playerViewModel) }
            composable(AppRoute.PLAYER) {
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
