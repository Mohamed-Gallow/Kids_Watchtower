package com.example.gomahrepoproject.main.AppTimeRangeBlocker

/**
 * Model describing a monitored app and the allowed time range for usage.
 */
import java.io.Serializable

data class AppTimeRange(
    val appName: String,
    val packageName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) : Serializable
