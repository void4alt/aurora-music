package com.auramusic.data.repository

import com.auramusic.BuildConfig
import com.auramusic.models.SearchFilter
import com.auramusic.models.Song
import com.auramusic.models.SongSource
import com.auramusic.network.DurationParser
import com.auramusic.network.YouTubeApiService
import com.auramusic.network.dto.VideoDto
import com.auramusic.repository.MusicRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepositoryImpl @Inject constructor(
    private val api: YouTubeApiService
) : MusicRepository {

    private val key: String
        get() = BuildConfig.YOUTUBE_API_KEY

    override suspend fun search(query: String, filter: SearchFilter): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            runCatching {
                checkConfigured()
                val effectiveQuery = if (filter == SearchFilter.MUSIC) "$query music" else query
                val response = api.search(
                    key = key,
                    query = effectiveQuery,
                    part = "snippet",
                    type = if (filter == SearchFilter.PLAYLIST) "playlist" else "video",
                    maxResults = 25,
                    videoDuration = null,
                    eventType = if (filter == SearchFilter.LIVE) "live" else null,
                    pageToken = null
                )
                val songs = response.items.orEmpty().mapNotNull { item ->
                    val videoId = item.id?.videoId ?: return@mapNotNull null
                    val snippet = item.snippet ?: return@mapNotNull null
                    Song(
                        id = "yt_$videoId",
                        title = snippet.title?.trim()?.ifBlank { "Untitled" } ?: "Untitled",
                        artist = snippet.channelTitle?.trim()?.ifBlank { "Unknown artist" }
                            ?: "Unknown artist",
                        thumbnailUrl = snippet.thumbnails?.high?.url
                            ?: snippet.thumbnails?.medium?.url
                            ?: snippet.thumbnails?.defaultThumbnail?.url,
                        videoId = videoId,
                        source = SongSource.YOUTUBE
                    )
                }
                hydrateDurations(songs)
            }
        }

    override suspend fun trending(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            checkConfigured()
            val response = api.popular(
                key = key,
                part = "snippet,contentDetails",
                chart = "mostPopular",
                regionCode = "US",
                maxResults = 20
            )
            response.items.orEmpty().mapNotNull { it.toSong() }
        }
    }

    override suspend fun suggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank() || key == MISSING_KEY) return@withContext emptyList()
        runCatching {
            api.search(
                key = key,
                query = query,
                part = "snippet",
                type = "video",
                maxResults = 6,
                videoDuration = null,
                eventType = null,
                pageToken = null
            ).items.orEmpty().mapNotNull { it.snippet?.title?.trim()?.takeIf { t -> t.isNotBlank() } }
        }.getOrDefault(emptyList())
    }

    private suspend fun hydrateDurations(songs: List<Song>): List<Song> {
        val durations = mutableMapOf<String, Long>()
        songs.mapNotNull { it.videoId }.distinct().chunked(50).forEach { chunk ->
            runCatching {
                api.videos(key = key, part = "contentDetails", id = chunk.joinToString(","))
            }.getOrNull()?.items?.forEach { dto ->
                dto.id?.let { id -> durations[id] = DurationParser.parse(dto.contentDetails?.duration) }
            }
        }
        return songs.map { it.copy(durationMs = durations[it.videoId] ?: 0L) }
    }

    private fun VideoDto.toSong(): Song = Song(
        id = "yt_${id.orEmpty()}",
        title = snippet?.title?.trim()?.ifBlank { "Untitled" } ?: "Untitled",
        artist = snippet?.channelTitle?.trim()?.ifBlank { "Unknown artist" } ?: "Unknown artist",
        thumbnailUrl = snippet?.thumbnails?.high?.url
            ?: snippet?.thumbnails?.medium?.url
            ?: snippet?.thumbnails?.defaultThumbnail?.url,
        durationMs = DurationParser.parse(contentDetails?.duration),
        videoId = id,
        source = SongSource.YOUTUBE
    )

    private fun checkConfigured() {
        check(key != MISSING_KEY) {
            "YouTube Data API key is missing. Add YOUTUBE_API_KEY to local.properties."
        }
        check(key.isNotBlank()) { "YouTube Data API key is blank." }
    }

    private companion object {
        const val MISSING_KEY = "MISSING_YOUTUBE_API_KEY"
    }
}
