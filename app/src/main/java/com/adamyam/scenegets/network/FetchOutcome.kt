package com.adamyam.scenegets.network

sealed class FetchOutcome {
    data class Success(val body: String, val etag: String? = null) : FetchOutcome()
    data object NotModified : FetchOutcome()
    data class Failure(
        val message: String,
        val isNotFound: Boolean,
        val cause: Throwable? = null
    ) : FetchOutcome()
}
