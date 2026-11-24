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
 * Turns a YouTube video id into a directly playable audio URL.
 *
 * Order of attack:
 *  1. InnerTube player endpoint (ANDROID / iOS / VR clients) — returns plain googlevideo URLs.
 *  2. Piped public instances — return proxied audio streams, no cipher work needed.
 *  3. Invidious public instances — same idea, proxied through the instance.
 *
 * Results are cached for an hour so queue transitions do not re-resolve.
 */
@Singleton
class StreamResolver @Inject constructor(
    private val innerTube: InnerTubeClient,
    private val client: OkHttpClient,
    private val gson: Gson
) {

    private data class Cached(val stream: AudioStream, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, Cached>()

    private val fastClient: OkHttpClient = client.newBuilder()
        .callTimeout(6, TimeUnit.SECONDS)
        .build()

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.leptons.xyz",
        "https://api.piped.private.coffee",
        "https://pipedapi.drgns.space"
    )

    private val invidiousInstances = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yewtu.be",
        "https://invidious.f5.si",
        "https://iv.melmac.space"
    )

    suspend fun resolve(videoId: String): Result<AudioStream> = runCatching {
        cache[videoId]?.takeIf { it.expiresAt > System.currentTimeMillis() }
            ?.let { return@runCatching it.stream }

        val direct = innerTube.player(videoId).sortedByDescending(::score)
        firstSuccess(direct.map { candidate -> suspend { candidate.takeIf { isPlayable(it) } } })
            ?.let { return@runCatching store(it) }

        fromPiped(videoId)?.let { return@runCatching store(it) }
        fromInvidious(videoId)?.let { return@runCatching store(it) }

        error("No playable audio stream found for video $videoId")
    }

    /** Blocking variant used by the ExoPlayer data-source resolver on its loader thread. */
    fun resolveBlocking(videoId: String, timeoutMs: Long = 60_000L): AudioStream? =
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) { resolve(videoId).getOrNull() }
        }

    fun invalidate(videoId: String) {
        cache.remove(videoId)
    }

    private fun store(stream: AudioStream): AudioStream {
        cache[stream.videoId] = Cached(stream, System.currentTimeMillis() + CACHE_TTL_MS)
        return stream
    }

    private fun score(stream: AudioStream): Int = when (stream.itag) {
        140 -> 100
        251 -> 95
        250 -> 90
        249 -> 85
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

    private suspend fun fromPiped(videoId: String): AudioStream? = firstSuccess(
        pipedInstances.map { base -> suspend { pipedFrom(base, videoId) } }
    ) { isPlayable(it) }

    private suspend fun pipedFrom(base: String, videoId: String): AudioStream? {
        val json = getJson("$base/streams/$videoId") ?: return null
        val streams = json.arr("audioStreams") ?: return null
        val best = streams.mapNotNull { it.asJsonObject }
            .filter { it.str("url") != null }
            .maxByOrNull(::pipedScore) ?: return null
        val url = best.str("url") ?: return null
        return AudioStream(
            videoId = videoId,
            url = url,
            itag = best.str("itag")?.toIntOrNull() ?: 0,
            mimeType = best.str("mimeType") ?: "audio/mp4",
            bitrate = best.str("bitrate")?.toLongOrNull() ?: 0L,
            durationMs = (json.str("duration")?.toLongOrNull() ?: 0L) * 1000L
        )
    }

    private fun pipedScore(obj: JsonObject): Int {
        val itag = obj.str("itag")?.toIntOrNull() ?: 0
        val base = when (itag) {
            140 -> 100
            251 -> 95
            250 -> 90
            249 -> 85
            else -> 50
        }
        return base + if (obj.str("mimeType").orEmpty().startsWith("audio/mp4")) 10 else 0
    }

    private suspend fun fromInvidious(videoId: String): AudioStream? = firstSuccess(
        invidiousInstances.map { base -> suspend { invidiousFrom(base, videoId) } }
    ) { isPlayable(it) }

    private suspend fun invidiousFrom(base: String, videoId: String): AudioStream? {
        val json = getJson(
            "$base/api/v1/videos/$videoId?fields=adaptiveFormats,lengthSeconds&local=true"
        ) ?: return null
        val streams = json.arr("adaptiveFormats") ?: return null
        val candidates = streams.mapNotNull { it.asJsonObject }
            .filter {
                it.str("type").orEmpty().startsWith("audio/") && it.str("url") != null
            }
        val best = candidates.maxByOrNull(::invidiousScore) ?: return null
        return AudioStream(
            videoId = videoId,
            url = best.str("url") ?: return null,
            itag = best.str("itag")?.toIntOrNull() ?: 0,
            mimeType = best.str("type")?.substringBefore(";") ?: "audio/mp4",
            bitrate = best.str("bitrate")?.toLongOrNull() ?: 0L,
            durationMs = (json.str("lengthSeconds")?.toLongOrNull() ?: 0L) * 1000L
        )
    }

    private suspend fun <T> firstSuccess(
        tasks: List<suspend () -> T?>,
        valid: suspend (T) -> Boolean = { true }
    ): T? = coroutineScope {
        val deferred = tasks.map { async(Dispatchers.IO) { it() } }
        for (task in deferred) {
            val value = task.await()
            if (value != null && valid(value)) {
                deferred.forEach { it.cancel() }
                return@coroutineScope value
            }
        }
        null
    }

    private fun invidiousScore(obj: JsonObject): Int {
        val itag = obj.str("itag")?.toIntOrNull() ?: 0
        val base = when (itag) {
            140 -> 100
            251 -> 95
            250 -> 90
            249 -> 85
            else -> 50
        }
        return base + if (obj.str("type").orEmpty().startsWith("audio/mp4")) 10 else 0
    }

    private suspend fun getJson(url: String): JsonObject? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuroraMusic/2.0")
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
