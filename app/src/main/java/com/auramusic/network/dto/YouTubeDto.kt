package com.auramusic.network.dto

import com.google.gson.annotations.SerializedName

data class YouTubeSearchResponse(
    val items: List<SearchItemDto>? = null,
    val nextPageToken: String? = null
)

data class SearchItemDto(
    val id: VideoIdDto? = null,
    val snippet: SnippetDto? = null
)

data class VideoIdDto(
    val videoId: String? = null,
    val playlistId: String? = null
)

data class SnippetDto(
    val title: String? = null,
    val description: String? = null,
    val channelTitle: String? = null,
    val thumbnails: ThumbnailsDto? = null
)

data class ThumbnailsDto(
    @SerializedName("default") val defaultThumbnail: ThumbnailDto? = null,
    val medium: ThumbnailDto? = null,
    val high: ThumbnailDto? = null
)

data class ThumbnailDto(val url: String? = null)

data class YouTubeVideoResponse(val items: List<VideoDto>? = null)

data class VideoDto(
    val id: String? = null,
    val snippet: SnippetDto? = null,
    val contentDetails: ContentDetailsDto? = null
)

data class ContentDetailsDto(val duration: String? = null)
