package com.auramusic.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Playback feedback emitted by the service, which owns the actual player. */
@Singleton
class PlaybackStatus @Inject constructor() {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(message: String) {
        _events.tryEmit(message)
    }
}
