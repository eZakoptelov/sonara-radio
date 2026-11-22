package com.example.sonora.data.remote

import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.presentation.localization.uiText
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Public Radio Browser catalogue. No API key is required.
 * Every catalogue/search request is restricted to the language selected by the user.
 */
class RadioBrowserApi {
    private val hosts = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .callTimeout(22, TimeUnit.SECONDS)
        .build()

    /** Popular stations in the selected language. */
    fun popular(language: RadioLanguage, limit: Int = 10, offset: Int = 0): List<RadioStation> =
        byLanguage(language, limit, offset)

    /** Stations speaking the selected language. */
    fun byLanguage(
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation> {
        val encoded = pathSegment(language.apiValue)
        return getStations(
            "/json/stations/bylanguage/$encoded?limit=$limit&offset=$offset&order=clickcount&reverse=true&hidebroken=true",
            language
        )
    }

    /** Stations from a selected country that also speak the chosen language. */
    fun byCountryCode(
        countryCode: String,
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation> {
        val code = countryCode.trim().uppercase(Locale.ROOT)
        require(code.length == 2) {
            uiText(language, "error.invalid_country")
        }
        val languageParam = queryParam(language.apiValue)
        return getStations(
            "/json/stations/search?countrycode=$code&language=$languageParam&limit=$limit&offset=$offset&order=clickcount&reverse=true&hidebroken=true",
            language
        )
    }

    /** Search by station name, restricted to the selected broadcast language. */
    fun search(
        query: String,
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()

        val encoded = queryParam(normalized)
        val languageParam = queryParam(language.apiValue)
        return getStations(
            "/json/stations/search?name=$encoded&language=$languageParam&limit=$limit&offset=$offset&order=clickcount&reverse=true&hidebroken=true",
            language
        )
    }

    /** Genre selection with a language constraint. */
    fun byTag(
        tag: String,
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation> {
        val encodedTag = queryParam(tag.trim().lowercase(Locale.ROOT))
        val languageParam = queryParam(language.apiValue)
        return getStations(
            "/json/stations/search?tag=$encodedTag&language=$languageParam&limit=$limit&offset=$offset&order=clickcount&reverse=true&hidebroken=true",
            language
        )
    }

    private fun getStations(path: String, language: RadioLanguage): List<RadioStation> {
        var lastError: Exception? = null

        for (host in hosts) {
            try {
                val request = Request.Builder()
                    .url(host + path)
                    .header("User-Agent", "SONARA/2.3.9 (Android internet radio app)")
                    .header("Accept", "application/json")
                    .build()

                val stations = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(uiText(language, "error.catalog_http", response.code))
                    }
                    val body = response.body?.string()
                        ?: throw IOException(uiText(language, "error.catalog_empty"))
                    parseStations(JSONArray(body), language)
                }
                return stations
            } catch (exception: Exception) {
                lastError = exception
            }
        }

        throw IOException(
            uiText(language, "error.catalog_connection"),
            lastError
        )
    }

    private fun parseStations(array: JSONArray, language: RadioLanguage): List<RadioStation> {
        val result = ArrayList<RadioStation>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val station = item.toStation() ?: continue
            // Defensive check in case a catalogue mirror returns stations in another language.
            if (language.matchesStationLanguage(station.language)) result += station
        }
        return result.distinctBy { it.id }
    }

    private fun JSONObject.toStation(): RadioStation? {
        val id = safeString("stationuuid")
        val name = safeString("name")
        val stream = safeString("url_resolved").ifBlank { safeString("url") }
        if (id.isBlank() || name.isBlank() || stream.isBlank()) return null
        if (!stream.startsWith("http://") && !stream.startsWith("https://")) return null

        return RadioStation(
            id = id,
            name = name,
            streamUrl = stream,
            favicon = safeString("favicon"),
            country = safeString("country"),
            countryCode = safeString("countrycode"),
            language = safeString("language"),
            tags = safeString("tags"),
            codec = safeString("codec"),
            bitrate = optInt("bitrate", 0),
            homepage = safeString("homepage")
        )
    }

    private fun JSONObject.safeString(key: String): String {
        val value = optString(key, "").trim()
        return if (value == "null") "" else value
    }

    private fun pathSegment(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun queryParam(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
