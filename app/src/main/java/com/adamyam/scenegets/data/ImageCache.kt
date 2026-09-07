package com.adamyam.scenegets.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adamyam.scenegets.network.SceneFlixHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object ImageCache {
    private const val TARGET_SIZE_PX = 150
    private const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    private const val MAX_CONCURRENT_DOWNLOADS = 4
    private const val BUFFER_SIZE = 16 * 1024
    private const val MAX_CACHE_BYTES = 40L * 1024 * 1024
    private const val TRIM_TARGET_BYTES = 30L * 1024 * 1024
    private val locks = ConcurrentHashMap<String, Any>()
    private val evictionLock = Any()
    private val trackedCacheBytes = AtomicLong(-1L)

    private sealed class DownloadOutcome {
        data class Success(val bytes: ByteArray, val etag: String?) : DownloadOutcome()
        data object NotModified : DownloadOutcome()
        data object Failed : DownloadOutcome()
    }

    private fun isExpired(file: File): Boolean =
        System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MILLIS

    suspend fun loadAll(context: Context, urls: List<String>): Map<String, Bitmap> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
        urls.asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .map { url ->
                async {
                    semaphore.withPermit { load(context, url)?.let { url to it } }
                }
            }
            .toList()
            .awaitAll()
            .filterNotNull()
            .toMap()
    }

    suspend fun load(context: Context, url: String, targetSizePx: Int = TARGET_SIZE_PX): Bitmap? =
        withContext(Dispatchers.IO) {
            val file = cacheFile(context, url)
            ensureCached(file, url)
            if (!file.isFile || file.length() <= 0L) return@withContext null
            decodeSampled(file, targetSizePx)
        }

    fun loadEncoded(context: Context, url: String, targetSizePx: Int): ByteArray? {
        if (!isHttpUrl(url)) return null
        val file = cacheFile(context, url)
        ensureCached(file, url)
        if (!file.isFile || file.length() <= 0L) return null
        val bitmap = decodeSampled(file, targetSizePx) ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) return null
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun ensureCached(file: File, url: String) {
        if (!isHttpUrl(url)) return
        val lock = locks.computeIfAbsent(file.absolutePath) { Any() }
        synchronized(lock) {
            if (!file.exists()) {
                val outcome = download(url, null)
                if (outcome is DownloadOutcome.Success) persist(file, outcome)
                return
            }
            if (!isExpired(file)) return
            when (val outcome = download(url, readEtag(file))) {
                is DownloadOutcome.Success -> persist(file, outcome)
                DownloadOutcome.NotModified -> {
                    runCatching { file.setLastModified(System.currentTimeMillis()) }
                }
                DownloadOutcome.Failed -> Unit
            }
        }
    }

    private fun persist(file: File, outcome: DownloadOutcome.Success) {
        val parent = file.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) return
        val previousSize = if (file.isFile) file.length() else 0L
        val temp = File(parent, file.name + ".tmp")
        val tempEtag = File(parent, file.name + ".etag.tmp")
        try {
            FileOutputStream(temp).use { output -> output.write(outcome.bytes); output.fd.sync() }
            moveAtomically(temp, file)
            if (outcome.etag.isNullOrBlank()) {
                tempEtag.delete()
                etagFile(file).delete()
            } else {
                FileOutputStream(tempEtag).use { output ->
                    output.write(outcome.etag.toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                runCatching { moveAtomically(tempEtag, etagFile(file)) }
                    .onFailure { etagFile(file).delete() }
                    .getOrThrow()
            }
            file.setLastModified(System.currentTimeMillis())
            trackSizeDelta(parent, file.length() - previousSize)
            evictIfOverCap(parent)
        } catch (_: Exception) {
            temp.delete()
            tempEtag.delete()
        }
    }

    private fun trackSizeDelta(dir: File, delta: Long) {
        if (trackedCacheBytes.get() < 0) {
            synchronized(evictionLock) {
                if (trackedCacheBytes.get() < 0) {
                    trackedCacheBytes.set(dir.listFiles()?.sumOf { it.length() } ?: 0L)
                    return
                }
            }
        }
        trackedCacheBytes.updateAndGet { (it + delta).coerceAtLeast(0L) }
    }

    private fun evictIfOverCap(dir: File) {
        if (trackedCacheBytes.get() <= MAX_CACHE_BYTES) return
        synchronized(evictionLock) {
            if (trackedCacheBytes.get() <= MAX_CACHE_BYTES) return
            val imageFiles = dir.listFiles { candidate -> !candidate.name.endsWith(".etag") }
                ?.sortedBy { it.lastModified() }
                ?: return
            var freed = 0L
            for (imageFile in imageFiles) {
                if (trackedCacheBytes.get() - freed <= TRIM_TARGET_BYTES) break
                val size = imageFile.length()
                if (imageFile.delete()) {
                    etagFile(imageFile).delete()
                    freed += size
                }
            }
            if (freed > 0L) trackedCacheBytes.updateAndGet { (it - freed).coerceAtLeast(0L) }
        }
    }

    private fun moveAtomically(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun decodeSampled(file: File, targetSizePx: Int): Bitmap? = runCatching {
        if (targetSizePx <= 0 || file.length() > MAX_IMAGE_BYTES) return@runCatching null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= targetSizePx &&
            bounds.outHeight / (sampleSize * 2) >= targetSizePx
        ) sampleSize *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }.getOrNull()

    private fun download(url: String, etag: String?): DownloadOutcome = runCatching {
        if (!isHttpUrl(url)) return@runCatching DownloadOutcome.Failed
        val builder = Request.Builder().url(url).get()
        if (!etag.isNullOrBlank()) builder.header("If-None-Match", etag)
        SceneFlixHttpClient.client.newCall(builder.build()).execute().use { response ->
            when {
                response.code == 304 -> DownloadOutcome.NotModified
                !response.isSuccessful -> DownloadOutcome.Failed
                else -> {
                    val body = response.body ?: return@use DownloadOutcome.Failed
                    val length = body.contentLength()
                    if (length > MAX_IMAGE_BYTES) return@use DownloadOutcome.Failed
                    val input = body.byteStream()
                    val output = ByteArrayOutputStream(minOf(MAX_IMAGE_BYTES, if (length > 0) length.toInt() else 32 * 1024))
                    val buffer = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    input.use {
                        while (true) {
                            val read = it.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_IMAGE_BYTES) return@use DownloadOutcome.Failed
                            output.write(buffer, 0, read)
                        }
                    }
                    DownloadOutcome.Success(output.toByteArray(), response.header("ETag"))
                }
            }
        }
    }.getOrDefault(DownloadOutcome.Failed)

    private fun isHttpUrl(url: String): Boolean =
        runCatching {
            val uri = android.net.Uri.parse(url)
            uri.scheme == "http" || uri.scheme == "https"
        }.getOrDefault(false)

    private fun cacheFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, "scenegets_images").apply { mkdirs() }
        val name = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(dir, name)
    }

    private fun etagFile(file: File): File = File(file.parentFile, file.name + ".etag")

    private fun readEtag(file: File): String? {
        val sidecar = etagFile(file)
        if (!sidecar.isFile || sidecar.length() > 4096L) return null
        return runCatching { FileInputStream(sidecar).bufferedReader(Charsets.UTF_8).use { it.readText().trim() } }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
    }
}
