package com.adamyam.scenegets.widget.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Glance 위젯은 일반 Compose처럼 URL을 바로 Image로 그릴 수 없기 때문에,
 * 썸네일/앨범 이미지를 로컬 Bitmap 캐시로 내려받아 ImageProvider(Bitmap)으로 표시한다.
 */
object WidgetImageLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun load(context: Context, url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null

        val dir = File(context.cacheDir, "widget_images").apply { mkdirs() }
        val file = File(dir, sha256(url) + ".img")

        // 먼저 로컬 캐시를 사용해 위젯 갱신 때 이미지가 깜빡이지 않도록 한다.
        if (file.exists() && file.length() > 0L) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { return@withContext it }
        }

        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
                runCatching {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
                    }
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
