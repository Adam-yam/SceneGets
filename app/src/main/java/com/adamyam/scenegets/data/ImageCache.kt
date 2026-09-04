package com.adamyam.scenegets.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 뉴스 썸네일 / 앨범 커버처럼 URL로 오는 이미지를 다운로드해서
 * 앱 캐시 디렉터리에 저장해두고 Bitmap으로 돌려주는 간단한 이미지 캐시.
 *
 * Glance 위젯의 Composable 안에서는 네트워크 호출을 할 수 없기 때문에,
 * GlanceAppWidget.provideGlance()의 suspend 구간에서 미리 이 캐시를 통해
 * Bitmap을 받아온 뒤 Composable에 그대로 전달하는 방식으로 사용한다.
 */
object ImageCache {

    // 위젯 안에 들어가는 작은 썸네일이라 원본 해상도가 필요 없음.
    // 원본 그대로 디코딩하면(특히 뉴스 썸네일처럼 큰 이미지가 여러 개 있을 때)
    // RemoteViews 전송 용량 제한에 걸려 위젯 전체가 "콘텐츠를 표시할 수 없음"으로
    // 깨지기 때문에, 실제 표시 크기에 맞춰 다운샘플링해서 디코딩한다.
    private const val TARGET_SIZE_PX = 150

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** 여러 URL을 한 번에 로드해서 url -> Bitmap 맵으로 돌려준다. 실패한 URL은 맵에서 빠진다. */
    suspend fun loadAll(context: Context, urls: List<String>): Map<String, Bitmap> {
        val result = mutableMapOf<String, Bitmap>()
        urls.filter { it.isNotBlank() }.distinct().forEach { url ->
            load(context, url)?.let { bitmap -> result[url] = bitmap }
        }
        return result
    }

    suspend fun load(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = cacheFile(context, url)
        if (!file.exists()) {
            val bytes = download(url)
            if (bytes == null) return@withContext null
            runCatching { file.writeBytes(bytes) }
        }
        decodeSampled(file)
    }

    private fun decodeSampled(file: File): Bitmap? = runCatching {
        // 1) 실제 픽셀을 메모리에 올리지 않고 원본 크기만 먼저 확인
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        // 2) 목표 크기보다 커지지 않는 선에서 2의 배수로 축소 비율 계산
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= TARGET_SIZE_PX &&
            bounds.outHeight / (sampleSize * 2) >= TARGET_SIZE_PX
        ) {
            sampleSize *= 2
        }

        // 3) 축소된 크기로 디코딩 + 알파가 필요 없는 썸네일이라 RGB_565로 메모리 절반 절약
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }.getOrNull()

    private fun download(url: String): ByteArray? = runCatching {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.bytes() else null
        }
    }.getOrNull()

    private fun cacheFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, "scenegets_images").apply { mkdirs() }
        val name = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, name)
    }
}
