package com.auramusic.data.mapper

import com.auramusic.database.SongEntity
import com.auramusic.models.Song
import com.auramusic.models.SongSource

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
    videoId = videoId,
    mediaUrl = mediaUrl,
    source = source.name,
    addedAt = addedAt
)

fun SongEntity.toSong(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
    videoId = videoId,
    mediaUrl = mediaUrl,
    source = runCatching { SongSource.valueOf(source) }.getOrDefault(SongSource.YOUTUBE),
    addedAt = addedAt
)
