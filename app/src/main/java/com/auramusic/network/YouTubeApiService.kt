package com.auramusic.network

import com.auramusic.network.dto.YouTubeSearchResponse
import com.auramusic.network.dto.YouTubeVideoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun search(
        @Query("key") key: String,
        @Query("q") query: String,
        @Query("part") part: String,
        @Query("type") type: String,
        @Query("maxResults") maxResults: Int,
        @Query("videoDuration") videoDuration: String?,
        @Query("eventType") eventType: String?,
        @Query("pageToken") pageToken: String?
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun videos(
        @Query("key") key: String,
        @Query("part") part: String,
        @Query("id") id: String
    ): YouTubeVideoResponse

    @GET("videos")
    suspend fun popular(
        @Query("key") key: String,
        @Query("part") part: String,
        @Query("chart") chart: String,
        @Query("regionCode") regionCode: String,
        @Query("maxResults") maxResults: Int
    ): YouTubeVideoResponse
}
