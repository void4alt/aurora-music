package com.auramusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auramusic.data.StreamResolver
import com.auramusic.player.STREAM_HOST
import com.auramusic.player.STREAM_USER_AGENT
import com.auramusic.player.toMediaItem
import com.auramusic.player.toSong
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AuroraPlaybackService : MediaSessionService() {

    @Inject
    lateinit var settings: SettingsDataStore

    @Inject
    lateinit var streamResolver: StreamResolver

    @Inject
    lateinit var playbackStatus: PlaybackStatus

    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null
    private val attemptsByKey = mutableMapOf<String, Int>()
    private var pendingReconnect = false
    private var lastQueueKey: String? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(streamDataSourceFactory()))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val item = player.currentMediaItem
                val song = item?.toSong()
                val videoId = song?.videoId
                val key = item?.mediaId ?: videoId ?: "unknown"
                val attempts = attemptsByKey[key] ?: 0
                val detail = "Playback failed (${error.errorCode}): ${error.message ?: "source error"}"
                if (videoId == null || attempts >= MAX_ATTEMPTS) {
                    pendingReconnect = false
                    playbackStatus.emit(detail)
                    return
                }
                attemptsByKey[key] = attempts + 1
                pendingReconnect = true
                scope.launch(Dispatchers.IO) {
                    val stream = streamResolver.nextAlternative(videoId, song?.mediaUrl)
                        ?: streamResolver.refresh(videoId)
                        ?: streamResolver.loadFallbacks(videoId)
                            .let { streamResolver.nextAlternative(videoId, null) }
                    withContext(Dispatchers.Main) {
                        if (stream != null) {
                            applyStream(stream.url)
                        } else {
                            pendingReconnect = false
                            playbackStatus.emit(detail)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && pendingReconnect) {
                    pendingReconnect = false
                    playbackStatus.emit("Reconnected — playing now")
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason != Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) return
                val key = queueKey()
                if (key != lastQueueKey) {
                    lastQueueKey = key
                    attemptsByKey.clear()
                    pendingReconnect = false
                }
            }
        })

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

    private fun queueKey(): String =
        (0 until player.mediaItemCount).joinToString(",") { player.getMediaItemAt(it).mediaId }

    private fun applyStream(url: String) {
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        val current = player.currentMediaItem?.toSong() ?: return
        val items = (0 until player.mediaItemCount).map { i ->
            if (i == index) current.copy(mediaUrl = url).toMediaItem() else player.getMediaItemAt(i)
        }
        player.setMediaItems(items, index, player.currentPosition.coerceAtLeast(0L))
        player.prepare()
        player.play()
    }

    /**
     * Every YouTube item carries a placeholder URI. Right before a track is
     * loaded (including auto-advance through the queue) it is swapped for a fresh, directly
     * playable audio URL.
     */
    private fun streamDataSourceFactory(): ResolvingDataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(STREAM_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(mapOf("Accept-Encoding" to "identity"))
        val base = DefaultDataSource.Factory(this, httpFactory)
        return ResolvingDataSource.Factory(base) { dataSpec ->
            val uri = dataSpec.uri
            if (uri.host == STREAM_HOST) {
                val videoId = uri.lastPathSegment
                    ?: throw IOException("Missing video id in $uri")
                val stream = streamResolver.resolveBlocking(videoId)
                    ?: throw IOException("Unable to resolve an audio stream for $videoId")
                dataSpec.withUri(Uri.parse(stream.url))
            } else {
                dataSpec
            }
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

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
