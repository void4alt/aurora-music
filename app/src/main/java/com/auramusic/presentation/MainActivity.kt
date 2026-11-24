package com.auramusic.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auramusic.player.PlayerViewModel
import com.auramusic.presentation.navigation.AppNavigation
import com.auramusic.presentation.theme.AuroraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AuroraTheme {
                val playerViewModel: PlayerViewModel = hiltViewModel(this)
                val playerState by playerViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    playerViewModel.externalPlay.collect { song ->
                        song.videoId?.let { id ->
                            runCatching {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id"))
                                )
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    playerViewModel.message.collect { }
                }

                AppNavigation(
                    playerViewModel = playerViewModel,
                    playerState = playerState
                )
            }
        }
    }
}
