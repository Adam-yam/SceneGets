package com.adamyam.scenegets.data

/**
 * 위젯 화면에서 쓰는 상태. Loading/Loaded/Failed를 항상 명시적으로 구분한다.
 * Loaded의 isStale=true는 "갱신은 실패했지만 이전에 저장해둔 데이터는 있다"는 뜻으로,
 * 위젯이 빈 화면 대신 마지막 데이터 + 실패 표시를 보여줄 수 있게 한다.
 */
sealed class WidgetState<out T> {
    data object Loading : WidgetState<Nothing>()
    data class Loaded<T>(
        val data: T,
        val fetchedAt: Long,
        val isStale: Boolean,
        val errorMessage: String? = null,
        val contentUnchanged: Boolean = false
    ) : WidgetState<T>()
    data class Failed(val message: String) : WidgetState<Nothing>()
}
