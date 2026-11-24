package com.auramusic.models

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val videoId: String? = null,
    val mediaUrl: String? = null,
    val source: SongSource = SongSource.YOUTUBE,
    val addedAt: Long = System.currentTimeMillis()
) {
    val isPlayableInApp: Boolean
        get() = !mediaUrl.isNullOrBlank() || !videoId.isNullOrBlank()
}

enum class SongSource { YOUTUBE, DIRECT_URL, LOCAL_FILE }

data class Playlist(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Song>
)

enum class SearchFilter(val apiValue: String?) {
    ALL(null),
    MUSIC("music"),
    VIDEO("video"),
    LIVE("live"),
    PLAYLIST("playlist")
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val playbackSpeed: Float = 1f,
    val queue: List<Song> = emptyList()
)
