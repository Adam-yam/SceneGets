package com.adamyam.scenegets.models

import kotlinx.serialization.Serializable

@Serializable
data class NewsResponse(
    val updated: String = "",
    val articles: List<NewsArticle> = emptyList()
)

@Serializable
data class NewsArticle(
    val title: String,
    val url: String,
    val date: String = "",
    val source: String = "",
    val description: String? = null,
    val thumbnail: String? = null
)
