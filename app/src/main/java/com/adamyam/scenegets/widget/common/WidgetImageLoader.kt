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

    suspend fun loadCached(context: Context, url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        val file = cacheFile(context, url)
        if (!file.exists() || file.length() <= 0L) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)?.let { downsample(it, 120) }
    }

    suspend fun load(context: Context, url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null

        val file = cacheFile(context, url)

        // 먼저 로컬 캐시를 사용해 위젯 갱신 때 이미지가 깜빡이지 않도록 한다.
        if (file.exists() && file.length() > 0L) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { return@withContext it }
        }

        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
                // Glance/RemoteViews는 큰 Bitmap을 여러 장 넣으면 런처의
                // Binder 제한에 걸려 위젯 전체가 "컨텐츠를 표시할 수 없음"이 될 수 있다.
                // 뉴스/앨범 표지용으로 충분한 크기까지만 축소한다.
                val bitmap = downsample(decoded, 120)
                if (bitmap !== decoded) decoded.recycle()
                runCatching {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 82, out)
                    }
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, "widget_images").apply { mkdirs() }
        return File(dir, sha256(url) + ".img")
    }

    private fun downsample(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxSize && height <= maxSize) return source
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val w = maxOf(1, (width * scale).toInt())
        val h = maxOf(1, (height * scale).toInt())
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
