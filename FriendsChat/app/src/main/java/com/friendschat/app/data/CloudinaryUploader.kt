package com.friendschat.app.data

import android.content.Context
import android.net.Uri
import com.friendschat.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Uploads media to Cloudinary using an UNSIGNED upload preset — no server and no
 * billing required. Returns the hosted https URL of the file.
 */
object CloudinaryUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    suspend fun upload(context: Context, uri: Uri, fileName: String): String =
        withContext(Dispatchers.IO) {
            val cloudName = context.getString(R.string.cloudinary_cloud_name)
            val preset = context.getString(R.string.cloudinary_upload_preset)
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            // "auto" lets Cloudinary store images, video and raw files on one endpoint.
            val url = "https://api.cloudinary.com/v1_1/$cloudName/auto/upload"

            val fileBody = object : RequestBody() {
                override fun contentType() = mime.toMediaTypeOrNull()
                override fun writeTo(sink: BufferedSink) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        sink.writeAll(input.source())
                    } ?: error("Could not open the selected file")
                }
            }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", preset)
                .addFormDataPart("file", fileName, fileBody)
                .build()

            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val reason = runCatching {
                        JSONObject(text).getJSONObject("error").getString("message")
                    }.getOrDefault("HTTP ${response.code}")
                    error("Cloudinary upload failed: $reason")
                }
                JSONObject(text).optString("secure_url").ifBlank {
                    error("Cloudinary upload returned no URL")
                }
            }
        }
}
