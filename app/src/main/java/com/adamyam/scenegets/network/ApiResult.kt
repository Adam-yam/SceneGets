package com.adamyam.scenegets.network

/** 네트워크 + 파싱을 합친 최종 결과. 성공/실패를 항상 명시적으로 다룬다 (silent failure 금지). */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val fetchedAt: Long = System.currentTimeMillis()) : ApiResult<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isNotFound: Boolean = false
    ) : ApiResult<Nothing>()
}
