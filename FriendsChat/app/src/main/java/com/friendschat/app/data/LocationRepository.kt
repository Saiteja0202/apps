package com.friendschat.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Live country / city lookup backed by the free public CountriesNow API. The
 * country list is fetched once; cities are fetched on demand for the selected
 * country, so the city dropdown updates "in real time" whenever the country
 * changes. No API key, no billing.
 */
object LocationRepository {

    private const val BASE = "https://countriesnow.space/api/v0.1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** All country names, alphabetically. */
    suspend fun countries(): List<String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BASE/countries/iso").get().build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Couldn't load countries (HTTP ${resp.code})")
            val data = JSONObject(text).optJSONArray("data") ?: return@use emptyList()
            (0 until data.length())
                .mapNotNull { data.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }
                .sorted()
        }
    }

    /** Cities for [country], alphabetically (de-duplicated). */
    suspend fun cities(country: String): List<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().put("country", country).toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$BASE/countries/cities").post(body).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Couldn't load cities (HTTP ${resp.code})")
            val arr = JSONObject(text).optJSONArray("data") ?: return@use emptyList()
            (0 until arr.length())
                .map { arr.optString(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }
}
