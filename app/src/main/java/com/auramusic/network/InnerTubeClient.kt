package com.auramusic.network

import com.auramusic.models.SearchFilter
import com.auramusic.models.Song
import com.auramusic.models.SongSource
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** A resolved, directly playable audio stream. */
data class AudioStream(
    val videoId: String,
    val url: String,
    val itag: Int,
    val mimeType: String,
    val bitrate: Long,
    val durationMs: Long
)

/**
 * Talks to YouTube's internal InnerTube endpoints directly instead of the quota-metered
 * Data API. Search, suggestions, trending and player calls need no user API key, so there is
 * no 10 000 unit/day ceiling to run into.
 */
@Singleton
class InnerTubeClient @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {

    private val fastClient: OkHttpClient = client.newBuilder()
        .callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private data class ClientSpec(
        val name: String,
        val version: String,
        val userAgent: String,
        val numericId: String,
        val extra: Map<String, Any> = emptyMap()
    )

    private val web = ClientSpec(
        name = "WEB",
        version = "2.20241126.01.00",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        numericId = "1"
    )

    private val android = ClientSpec(
        name = "ANDROID",
        version = "20.10.38",
        userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip",
        numericId = "3",
        extra = mapOf(
            "androidSdkVersion" to 30,
            "osName" to "Android",
            "osVersion" to "11"
        )
    )

    private val ios = ClientSpec(
        name = "IOS",
        version = "19.45.4",
        userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)",
        numericId = "5",
        extra = mapOf(
            "deviceMake" to "Apple",
            "deviceModel" to "iPhone16,2",
            "osName" to "iPhone",
            "osVersion" to "18.1.0.22B83"
        )
    )

    private val androidVr = ClientSpec(
        name = "ANDROID_VR",
        version = "1.60.19",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.60.19 " +
            "(Linux; U; Android 12; GB) gzip",
        numericId = "28",
        extra = mapOf(
            "androidSdkVersion" to 32,
            "deviceMake" to "Oculus",
            "deviceModel" to "Quest 3",
            "osName" to "Android",
            "osVersion" to "12L"
        )
    )

    private val tvEmbedded = ClientSpec(
        name = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
        version = "2.0",
        userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/16.0 Safari/605.1.15",
        numericId = "85"
    )

    private val playerClients = listOf(android, ios, androidVr, tvEmbedded)

    suspend fun search(query: String, filter: SearchFilter): List<Song> = withContext(Dispatchers.IO) {
        val effective = if (filter == SearchFilter.MUSIC) "$query music" else query
        for (spec in listOf(web, android)) {
            val body = JsonObject().apply {
                add("context", buildContext(spec))
                addProperty("query", effective)
            }
            val response = runCatching { post("search", body, spec) }.getOrNull() ?: continue
            val songs = collect(response, "videoRenderer").mapNotNull { it.toSong() }
            if (songs.isNotEmpty()) return@withContext songs
        }
        emptyList()
    }

    suspend fun trending(): List<Song> = withContext(Dispatchers.IO) {
        for (spec in listOf(web, android)) {
            val body = JsonObject().apply {
                add("context", buildContext(spec))
                addProperty("browseId", "FEtrending")
            }
            val response = runCatching { post("browse", body, spec) }.getOrNull() ?: continue
            val songs = collect(response, "videoRenderer").mapNotNull { it.toSong() }
            if (songs.isNotEmpty()) return@withContext songs.take(30)
        }
        search("trending music", SearchFilter.MUSIC)
    }

    suspend fun suggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=en&q=" +
                URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", web.userAgent)
                .build()
            fastClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                val text = response.body?.string().orEmpty()
                val root = gson.fromJson(text, JsonElement::class.java)
                val items = root.asJsonArray.get(1).asJsonArray
                items.mapNotNull { it.asString }.distinct().take(8)
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Asks several client flavours for a playable audio stream. Clients that return a plain
     * `url` are used; signature-ciphered entries are skipped because the resolver has
     * proxy fallbacks that need no JavaScript deciphering.
     */
    suspend fun player(videoId: String): List<AudioStream> = withContext(Dispatchers.IO) {
        for (spec in playerClients) {
            val body = JsonObject().apply {
                add("context", buildContext(spec))
                addProperty("videoId", videoId)
                addProperty("contentCheckOk", true)
                addProperty("racyCheckOk", true)
            }
            val response = runCatching { post("player", body, spec, withKey = false) }.getOrNull()
                ?: continue
            val status = response.obj("playabilityStatus")?.str("status")
            if (!status.isNullOrBlank() && status != "OK") continue
            val streams = parseAudioStreams(response, videoId)
            if (streams.isNotEmpty()) return@withContext streams
        }
        emptyList()
    }

    private fun parseAudioStreams(root: JsonObject, videoId: String): List<AudioStream> {
        val streaming = root.obj("streamingData") ?: return emptyList()
        val duration = root.obj("videoDetails")?.str("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L
        val result = mutableListOf<AudioStream>()
        for (key in listOf("adaptiveFormats", "formats")) {
            val formats = streaming.arr(key) ?: continue
            formats.forEach { element ->
                val format = element.asJsonObject
                val mime = format.str("mimeType") ?: return@forEach
                if (!mime.startsWith("audio/")) return@forEach
                val url = format.str("url") ?: return@forEach
                result += AudioStream(
                    videoId = videoId,
                    url = url,
                    itag = format.str("itag")?.toIntOrNull() ?: 0,
                    mimeType = mime,
                    bitrate = format.str("bitrate")?.toLongOrNull() ?: 0L,
                    durationMs = duration
                )
            }
        }
        return result.distinctBy { it.itag }
    }

    private fun buildContext(spec: ClientSpec): JsonObject {
        val client = JsonObject().apply {
            addProperty("clientName", spec.name)
            addProperty("clientVersion", spec.version)
            addProperty("hl", "en")
            addProperty("gl", "US")
            addProperty("utcOffsetMinutes", 0)
            spec.extra.forEach { (key, value) ->
                when (value) {
                    is Int -> addProperty(key, value)
                    is Long -> addProperty(key, value)
                    is Boolean -> addProperty(key, value)
                    else -> addProperty(key, value.toString())
                }
            }
        }
        return JsonObject().apply { add("client", client) }
    }

    private fun post(
        path: String,
        body: JsonObject,
        spec: ClientSpec,
        withKey: Boolean = true
    ): JsonObject {
        val url = if (withKey) "$API_BASE$path?key=$PUBLIC_WEB_KEY&prettyPrint=false"
            else "$API_BASE$path?prettyPrint=false"
        val request = Request.Builder()
            .url(url)
            .post(gson.toJson(body).toRequestBody(JSON_MEDIA_TYPE))
            .header("User-Agent", spec.userAgent)
            .header("X-YouTube-Client-Name", spec.numericId)
            .header("X-YouTube-Client-Version", spec.version)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        fastClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            check(response.isSuccessful) { "InnerTube $path failed with HTTP ${response.code}" }
            return gson.fromJson(text, JsonObject::class.java)
        }
    }

    private fun collect(root: JsonElement, key: String): List<JsonObject> {
        val found = mutableListOf<JsonObject>()
        fun walk(element: JsonElement) {
            when {
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    obj.obj(key)?.let { found += it }
                    obj.entrySet().forEach { (name, child) -> if (name != key) walk(child) }
                }
                element.isJsonArray -> element.asJsonArray.forEach { walk(it) }
            }
        }
        walk(root)
        return found
    }

    private fun JsonObject.toSong(): Song? {
        val videoId = str("videoId") ?: return null
        val title = textOf("title") ?: return null
        val artist = textOf("ownerText")
            ?: textOf("longBylineText")
            ?: textOf("shortBylineText")
            ?: "Unknown artist"
        val thumbnail = obj("thumbnail")?.arr("thumbnails")
            ?.lastOrNull()?.asJsonObject?.str("url")
        return Song(
            id = "yt_$videoId",
            title = title,
            artist = artist,
            thumbnailUrl = thumbnail,
            durationMs = parseClock(textOf("lengthText")),
            videoId = videoId,
            source = SongSource.YOUTUBE
        )
    }

    private fun JsonObject.textOf(key: String): String? {
        val element = get(key) ?: return null
        if (element.isJsonPrimitive) return element.asString.takeIf { it.isNotBlank() }
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        obj.str("simpleText")?.takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.arr("runs") ?: return null
        return runs.joinToString("") { run -> run.asJsonObject.str("text").orEmpty() }
            .takeIf { it.isNotBlank() }
    }

    private fun parseClock(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val parts = value.trim().split(":")
        if (parts.any { it.toLongOrNull() == null }) return 0L
        var seconds = 0L
        for (part in parts) seconds = seconds * 60 + part.toLong()
        return seconds * 1000L
    }

    private fun JsonObject.obj(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arr(key: String): JsonArray? =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private companion object {
        const val API_BASE = "https://www.youtube.com/youtubei/v1/"
        const val PUBLIC_WEB_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
