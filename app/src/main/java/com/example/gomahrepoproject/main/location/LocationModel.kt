package com.example.gomahrepoproject.main.location

data class LocationModel(
    val latitude: String = "",
    val longitude: String = "",
    val timestamp: Long = 0L,
    val locationEnabled: Boolean = true
)