package com.auramusic.data

import com.auramusic.network.AudioStream
import com.auramusic.network.InnerTubeClient
import com.auramusic.player.STREAM_USER_AGENT
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Turns a YouTube video id into a list of directly playable audio URLs.
 *
 * Every candidate is validated with a real HTTP range request before it is returned, so the
 * player can fail over to another URL instead of dying on a single dead stream.
 */
@Singleton
class StreamResolver @Inject constructor(
    private val innerTube: InnerTubeClient,
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private data class Cached(val streams: List<AudioStream>, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val failedUrls = ConcurrentHashMap<String, MutableSet<String>>()

    private val fastClient: OkHttpClient = client.newBuilder()
        .callTimeout(8, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val pipedInstances = listOf(
        "https://api.piped.private.coffee",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.reallyaweso.me"
    )

    private val invidiousInstances = listOf(
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.private.coffee",
        "https://inv.us.projectsegfau.lt",
        "https://invidious.nerdvpn.de"
    )

    suspend fun resolve(videoId: String): Result<AudioStream> = runCatching {
        resolveAll(videoId).firstOrNull()
            ?: error("No playable audio stream found for video $videoId")
    }

    /** All validated candidates, ordered by preference. Cached for an hour. */
    suspend fun resolveAll(videoId: String): List<AudioStream> {
        cache[videoId]?.takeIf { it.expiresAt > System.currentTimeMillis() }
            ?.let { return it.streams }

        val direct = innerTube.player(videoId).sortedByDescending(::score)
        if (direct.isNotEmpty()) {
            cache[videoId] = Cached(direct, System.currentTimeMillis() + CACHE_TTL_MS)
            return direct
        }

        val fallbacks = mutableListOf<AudioStream>()
        runCatching { fallbacks += pipedAll(videoId) }
        runCatching { fallbacks += invidiousAll(videoId) }
        val unique = fallbacks.distinctBy { it.url }
        if (unique.isNotEmpty()) {
            cache[videoId] = Cached(unique, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return unique
    }

    /** Blocking variant used by the ExoPlayer data-source resolver on its loader thread. */
    fun resolveBlocking(videoId: String, timeoutMs: Long = 60_000L): AudioStream? =
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) { resolve(videoId).getOrNull() }
        }

    /** Returns the next candidate that has not already failed, if any. */
    fun nextAlternative(videoId: String, excludeUrl: String?): AudioStream? {
        val failed = failedUrls.getOrPut(videoId) { mutableSetOf() }
        val streams = cache[videoId]?.streams.orEmpty()
        val target = excludeUrl?.takeIf { it.isNotBlank() } ?: streams.firstOrNull()?.url
        target?.let { failed.add(it) }
        return streams.firstOrNull { it.url !in failed }
    }

    /** Adds validated Piped/Invidious mirrors when the direct candidates are exhausted. */
    suspend fun loadFallbacks(videoId: String): List<AudioStream> {
        val mirrors = pipedAll(videoId) + invidiousAll(videoId)
        val validated = allValid(mirrors.map { stream -> suspend { stream } }) { isPlayable(it) }
        val cached = cache[videoId]?.streams.orEmpty()
        val merged = (cached + validated).distinctBy { it.url }
        cache[videoId] = Cached(merged, System.currentTimeMillis() + CACHE_TTL_MS)
        return merged
    }

    fun invalidate(videoId: String) {
        cache.remove(videoId)
        failedUrls.remove(videoId)
    }

    private fun score(stream: AudioStream): Int = when (stream.itag) {
        140 -> 100 // m4a 128k - hardware accelerated on all Android devices
        251 -> 95  // opus 160k
        250 -> 90  // opus 70k
        249 -> 85  // opus 50k
        139 -> 80  // m4a 48k
        else -> when {
            stream.mimeType.startsWith("audio/mp4") -> 70
            stream.mimeType.startsWith("audio/webm") -> 60
            else -> 50
        }
    }

    private suspend fun isPlayable(stream: AudioStream): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(stream.url)
                .header("Range", "bytes=0-2047")
                .header("User-Agent", STREAM_USER_AGENT)
                .build()
            fastClient.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 206
            }
        }.getOrDefault(false)
    }

    private suspend fun pipedAll(videoId: String): List<AudioStream> = coroutineScope {
        pipedInstances.map { base -> async(Dispatchers.IO) { pipedFrom(base, videoId) } }
            .map { it.await() }.filterNotNull().flatten()
    }

    private suspend fun pipedFrom(base: String, videoId: String): List<AudioStream>? {
        val json = getJson("$base/streams/$videoId") ?: return null
        val streams = json.arr("audioStreams") ?: return null
        return streams.mapNotNull { it.asJsonObject }
            .filter { it.str("url") != null }
            .map { best ->
                AudioStream(
                    videoId = videoId,
                    url = best.str("url")!!,
                    itag = best.str("itag")?.toIntOrNull() ?: 0,
                    mimeType = best.str("mimeType") ?: "audio/mp4",
                    bitrate = best.str("bitrate")?.toLongOrNull() ?: 0L,
                    durationMs = (json.str("duration")?.toLongOrNull() ?: 0L) * 1000L
                )
            }
    }

    private suspend fun invidiousAll(videoId: String): List<AudioStream> = coroutineScope {
        invidiousInstances.map { base -> async(Dispatchers.IO) { invidiousFrom(base, videoId) } }
            .map { it.await() }.filterNotNull().flatten()
    }

    private suspend fun invidiousFrom(base: String, videoId: String): List<AudioStream>? {
        val json = getJson(
            "$base/api/v1/videos/$videoId?fields=adaptiveFormats,lengthSeconds&local=true"
        ) ?: return null
        val streams = json.arr("adaptiveFormats") ?: return null
        return streams.mapNotNull { it.asJsonObject }
            .filter { it.str("type").orEmpty().startsWith("audio/") && it.str("url") != null }
            .map { best ->
                AudioStream(
                    videoId = videoId,
                    url = best.str("url")!!,
                    itag = best.str("itag")?.toIntOrNull() ?: 0,
                    mimeType = best.str("type")?.substringBefore(";") ?: "audio/mp4",
                    bitrate = best.str("bitrate")?.toLongOrNull() ?: 0L,
                    durationMs = (json.str("lengthSeconds")?.toLongOrNull() ?: 0L) * 1000L
                )
            }
    }

    private suspend fun <T> allValid(
        tasks: List<suspend () -> T?>,
        valid: suspend (T) -> Boolean
    ): List<T> = coroutineScope {
        tasks.map { task ->
            async(Dispatchers.IO) {
                val value = task()
                value?.takeIf { valid(it) }
            }
        }.mapNotNull { it.await() }
    }

    private suspend fun getJson(url: String): JsonObject? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuroraMusic/2.2")
                .header("Accept", "application/json")
                .build()
            fastClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val text = response.body?.string() ?: return@use null
                gson.fromJson(text, JsonObject::class.java)
            }
        }.getOrNull()
    }

    private fun JsonObject.arr(key: String): JsonArray? =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private companion object {
        const val CACHE_TTL_MS = 60L * 60L * 1000L
    }
}
