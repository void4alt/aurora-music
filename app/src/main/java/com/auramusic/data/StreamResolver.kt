package com.auramusic.data

import com.auramusic.network.AudioStream
import com.auramusic.network.InnerTubeClient
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
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
        .callTimeout(8, TimeUnit.SECONDS)
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

        innerTube.player(videoId).maxByOrNull(::score)?.let { return@runCatching store(it) }

        fromPiped(videoId)?.let { return@runCatching store(it) }
        fromInvidious(videoId)?.let { return@runCatching store(it) }

        error("No playable audio stream found for video $videoId")
    }

    /** Blocking variant used by the ExoPlayer data-source resolver on its loader thread. */
    fun resolveBlocking(videoId: String, timeoutMs: Long = 30_000L): AudioStream? =
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) { resolve(videoId).getOrNull() }
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

    private suspend fun fromPiped(videoId: String): AudioStream? {
        for (base in pipedInstances) {
            val json = getJson("$base/streams/$videoId") ?: continue
            val streams = json.arr("audioStreams") ?: continue
            val best = streams.mapNotNull { it.asJsonObject }
                .filter { it.str("url") != null }
                .maxByOrNull(::pipedScore) ?: continue
            val url = best.str("url") ?: continue
            return AudioStream(
                videoId = videoId,
                url = url,
                itag = best.str("itag")?.toIntOrNull() ?: 0,
                mimeType = best.str("mimeType") ?: "audio/mp4",
                bitrate = best.str("bitrate")?.toLongOrNull() ?: 0L,
                durationMs = (json.str("duration")?.toLongOrNull() ?: 0L) * 1000L
            )
        }
        return null
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

    private suspend fun fromInvidious(videoId: String): AudioStream? {
        for (base in invidiousInstances) {
            val json = getJson(
                "$base/api/v1/videos/$videoId?fields=adaptiveFormats,lengthSeconds&local=true"
            ) ?: continue
            val streams = json.arr("adaptiveFormats") ?: continue
            val candidates = streams.mapNotNull { it.asJsonObject }
                .filter {
                    it.str("type").orEmpty().startsWith("audio/") && it.str("url") != null
                }
            val best = candidates.maxByOrNull(::invidiousScore) ?: continue
            return AudioStream(
                videoId = videoId,
                url = best.str("url") ?: continue,
                itag = best.str("itag")?.toIntOrNull() ?: 0,
                mimeType = best.str("type")?.substringBefore(";") ?: "audio/mp4",
                bitrate = best.str("bitrate")?.toLongOrNull() ?: 0L,
                durationMs = (json.str("lengthSeconds")?.toLongOrNull() ?: 0L) * 1000L
            )
        }
        return null
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
