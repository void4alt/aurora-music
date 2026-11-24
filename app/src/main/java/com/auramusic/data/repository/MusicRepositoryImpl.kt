package com.auramusic.data.repository

import com.auramusic.models.SearchFilter
import com.auramusic.models.Song
import com.auramusic.network.InnerTubeClient
import com.auramusic.repository.MusicRepository
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    private val innerTube: InnerTubeClient
) : MusicRepository {

    override suspend fun search(query: String, filter: SearchFilter): Result<List<Song>> =
        runCatching { innerTube.search(query, filter) }

    override suspend fun trending(): Result<List<Song>> =
        runCatching { innerTube.trending() }

    override suspend fun suggestions(query: String): List<String> =
        innerTube.suggestions(query)
}
