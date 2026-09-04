package com.adamyam.scenegets.network

/** 저수준 HTTP 결과. "성공적으로 응답이 왔는가"만 판단하고, JSON 파싱은 상위 Api 객체가 담당한다. */
sealed class FetchOutcome {
    data class Success(val body: String) : FetchOutcome()
    data class Failure(
        val message: String,
        val isNotFound: Boolean,
        val cause: Throwable? = null
    ) : FetchOutcome()
}
