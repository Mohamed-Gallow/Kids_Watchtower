package com.example.gomahrepoproject.main.notifications

data class ChatState(
    val isEnteringToken: Boolean = true,
    val remoteToken: String = "",
    val messageText: String = ""

)