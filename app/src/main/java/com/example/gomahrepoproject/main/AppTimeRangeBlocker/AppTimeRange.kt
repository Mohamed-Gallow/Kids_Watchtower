package com.example.gomahrepoproject.main.AppTimeRangeBlocker

/**
 * Model describing a monitored app and the allowed time range for usage.
 */
import java.io.Serializable

data class AppTimeRange(
    val appName: String = "",
    val packageName: String = "",
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0
) : Serializable
