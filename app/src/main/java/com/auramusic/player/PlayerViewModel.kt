package com.auramusic.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.auramusic.models.PlayerUiState
import com.auramusic.models.Song
import com.auramusic.repository.LibraryRepository
import com.auramusic.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: MediaControllerConnection,
    private val library: LibraryRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    val controller = connection.controller

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _externalPlay = MutableSharedFlow<Song>(extraBufferCapacity = 1)
    val externalPlay: SharedFlow<Song> = _externalPlay.asSharedFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    private var playerListener: Player.Listener? = null
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            controller.collectLatest { ctrl ->
                playerListener?.let { ctrl?.removeListener(it) }
                ticker?.cancel()
                if (ctrl == null) {
                    _state.value = PlayerUiState()
                    return@collectLatest
                }
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        refresh()
                        if (isPlaying) startTicker() else ticker?.cancel()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh()
                    override fun onPlaybackStateChanged(playbackState: Int) = refresh()
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refresh()
                    override fun onRepeatModeChanged(repeatMode: Int) = refresh()
                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) = refresh()
                }
                playerListener = listener
                ctrl.addListener(listener)
                refresh()
                if (ctrl.isPlaying) startTicker()
            }
        }
        viewModelScope.launch {
            settings.playbackSpeed.first().let { speed ->
                controller.value?.setPlaybackParameters(
                    androidx.media3.common.PlaybackParameters(speed)
                )
            }
        }
    }

    fun play(song: Song, queue: List<Song> = listOf(song)) {
        val ctrl = controller.value
        if (!song.isPlayableInApp) {
            _externalPlay.tryEmit(song)
            return
        }
        if (ctrl == null) {
            _message.tryEmit("Player is still connecting")
            return
        }
        val playable = queue.filter { it.isPlayableInApp }
        if (playable.isEmpty()) {
            _externalPlay.tryEmit(song)
            return
        }
        val startIndex = playable.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        ctrl.setMediaItems(playable.map { it.toMediaItem() }, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
        viewModelScope.launch { library.recordPlay(song) }
    }

    fun togglePlay() {
        val ctrl = controller.value ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipNext() = controller.value?.seekToNextMediaItem()
    fun skipPrevious() = controller.value?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) { controller.value?.seekTo(positionMs) }

    fun setShuffle(enabled: Boolean) {
        controller.value?.shuffleModeEnabled = enabled
        viewModelScope.launch { settings.setShuffle(enabled) }
    }

    fun cycleRepeat() {
        val ctrl = controller.value ?: return
        val next = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        ctrl.repeatMode = next
        viewModelScope.launch { settings.setRepeatMode(next) }
    }

    fun setSpeed(speed: Float) {
        controller.value?.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed))
        viewModelScope.launch { settings.setPlaybackSpeed(speed) }
    }

    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch { settings.setSleepTimer(minutes) }
    }

    fun clearSleepTimer() {
        viewModelScope.launch { settings.setSleepTimer(0) }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { library.toggleFavorite(song) }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(500L)
                refresh()
            }
        }
    }

    private fun refresh() {
        val ctrl = controller.value ?: return
        val current = ctrl.currentMediaItem?.toSong()
        val queue = (0 until ctrl.mediaItemCount).mapNotNull { index ->
            ctrl.getMediaItemAt(index).toSong()
        }
        _state.value = PlayerUiState(
            isPlaying = ctrl.isPlaying,
            currentSong = current,
            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
            durationMs = if (ctrl.duration == C.TIME_UNSET) (current?.durationMs ?: 0L) else ctrl.duration,
            shuffle = ctrl.shuffleModeEnabled,
            repeatMode = ctrl.repeatMode,
            playbackSpeed = ctrl.playbackParameters.speed,
            queue = queue
        )
    }
}
