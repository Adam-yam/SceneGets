package com.adamyam.scenegets.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 조용히 null을 반환하던 기존 fetchText() 문제를 반복하지 않기 위해,
 * 성공/실패(404 포함)/네트워크 오류를 명시적으로 구분해서 반환한다.
 */
object SceneFlixHttpClient {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getRaw(url: String, etag: String? = null): FetchOutcome = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url).get()
            if (!etag.isNullOrBlank()) {
                requestBuilder.header("If-None-Match", etag)
            }
            client.newCall(requestBuilder.build()).execute().use { response ->
                when {
                    response.code == 304 -> FetchOutcome.NotModified
                    response.isSuccessful -> {
                        val body = response.body?.string()
                        if (body.isNullOrBlank()) {
                            FetchOutcome.Failure("응답 본문이 비어있음", isNotFound = false)
                        } else {
                            FetchOutcome.Success(body, response.header("ETag"))
                        }
                    }
                    response.code == 404 -> FetchOutcome.Failure("404 Not Found", isNotFound = true)
                    else -> FetchOutcome.Failure("HTTP ${response.code}", isNotFound = false)
                }
            }
        } catch (e: IOException) {
            FetchOutcome.Failure(e.message ?: "네트워크 오류", isNotFound = false, cause = e)
        }
    }
}
