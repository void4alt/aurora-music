package com.auramusic.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auramusic.playback.AuroraPlaybackService
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val token = SessionToken(context, ComponentName(context, AuroraPlaybackService::class.java))
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    init {
        val future = MediaController.Builder(context, token).buildAsync()
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) {
                    _controller.value = result
                }

                override fun onFailure(t: Throwable) {
                    _controller.value = null
                }
            },
            MoreExecutors.directExecutor()
        )
    }
}
