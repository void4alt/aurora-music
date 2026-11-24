package com.auramusic.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.auramusic.models.Song
import com.auramusic.models.SongSource

private const val KEY_SONG_ID = "songId"
private const val KEY_TITLE = "title"
private const val KEY_ARTIST = "artist"
private const val KEY_THUMB = "thumb"
private const val KEY_DURATION = "duration"
private const val KEY_VIDEO_ID = "videoId"
private const val KEY_MEDIA_URL = "mediaUrl"
private const val KEY_SOURCE = "source"

/** Custom scheme resolved to a real audio URL by the playback data source. */
const val STREAM_SCHEME = "aurastream"

fun Song.streamUri(): Uri? =
    mediaUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        ?: videoId?.takeIf { it.isNotBlank() }?.let { Uri.parse("$STREAM_SCHEME://yt/$it") }

fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putString(KEY_SONG_ID, id)
        putString(KEY_TITLE, title)
        putString(KEY_ARTIST, artist)
        putString(KEY_THUMB, thumbnailUrl)
        putLong(KEY_DURATION, durationMs)
        putString(KEY_VIDEO_ID, videoId)
        putString(KEY_MEDIA_URL, mediaUrl)
        putString(KEY_SOURCE, source.name)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUri())
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toSong(): Song? {
    val extras = mediaMetadata.extras ?: return null
    val id = extras.getString(KEY_SONG_ID) ?: mediaId
    return Song(
        id = id,
        title = extras.getString(KEY_TITLE) ?: mediaMetadata.title?.toString() ?: "Untitled",
        artist = extras.getString(KEY_ARTIST) ?: mediaMetadata.artist?.toString() ?: "Unknown artist",
        thumbnailUrl = extras.getString(KEY_THUMB),
        durationMs = extras.getLong(KEY_DURATION, 0L),
        videoId = extras.getString(KEY_VIDEO_ID),
        mediaUrl = extras.getString(KEY_MEDIA_URL),
        source = runCatching { SongSource.valueOf(extras.getString(KEY_SOURCE) ?: "YOUTUBE") }
            .getOrDefault(SongSource.YOUTUBE)
    )
}
