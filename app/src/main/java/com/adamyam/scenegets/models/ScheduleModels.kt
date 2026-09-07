package com.adamyam.scenegets.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val updated: String = "",
    val events: List<ScheduleEvent> = emptyList()
)

@Serializable
data class ScheduleEvent(
    val date: String,
    val time: String = "",
    val title: String,
    val detail: String = "",
    val type: String = "",
    val source: String = ""
)
