package com.auramusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auramusic.data.StreamResolver
import com.auramusic.player.STREAM_SCHEME
import com.auramusic.presentation.MainActivity
import com.auramusic.settings.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuroraPlaybackService : MediaSessionService() {

    @Inject
    lateinit var settings: SettingsDataStore

    @Inject
    lateinit var streamResolver: StreamResolver

    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(streamDataSourceFactory()))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val sessionIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(activityPendingIntent)
            .build()

        scope.launch {
            settings.sleepTimerMinutes
                .distinctUntilChanged()
                .collect { minutes ->
                    sleepJob?.cancel()
                    if (minutes > 0) {
                        sleepJob = scope.launch {
                            delay(minutes * 60_000L)
                            player.pause()
                        }
                    }
                }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * Every YouTube item carries an `aurastream://yt/<videoId>` URI. Right before a track is
     * loaded (including auto-advance through the queue) it is swapped for a fresh, directly
     * playable audio URL.
     */
    private fun streamDataSourceFactory(): ResolvingDataSource.Factory =
        ResolvingDataSource.Factory(DefaultDataSource.Factory(this)) { dataSpec ->
            val uri = dataSpec.uri
            if (uri.scheme == STREAM_SCHEME) {
                val videoId = uri.lastPathSegment
                    ?: throw IOException("Missing video id in $uri")
                val stream = streamResolver.resolveBlocking(videoId)
                    ?: throw IOException("Unable to resolve an audio stream for $videoId")
                dataSpec.withUri(Uri.parse(stream.url))
            } else {
                dataSpec
            }
        }

    override fun onDestroy() {
        sleepJob?.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
