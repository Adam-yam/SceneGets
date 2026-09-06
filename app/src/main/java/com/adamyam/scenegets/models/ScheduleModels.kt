package com.adamyam.scenegets.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val updated: String = "",
    val events: List<ScheduleEvent> = emptyList()
)

@Serializable
data class ScheduleEvent(
    val date: String,          // "yyyy-MM-dd"
    val time: String = "",     // "HH:mm" 또는 시간 미정 시 빈 문자열
    val title: String,
    val detail: String = "",
    val type: String = "",     // broadcast / radio / event / fansign / concert / notice ...
    val source: String = ""
)
