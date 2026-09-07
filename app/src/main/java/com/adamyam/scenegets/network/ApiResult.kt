package com.adamyam.scenegets.network

sealed class ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val fetchedAt: Long = System.currentTimeMillis(),
        val etag: String? = null
    ) : ApiResult<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isNotFound: Boolean = false
    ) : ApiResult<Nothing>()
}
