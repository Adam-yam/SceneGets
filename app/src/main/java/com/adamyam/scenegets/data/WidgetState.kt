package com.adamyam.scenegets.data

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
