package com.example.gomahrepoproject.main.notifications

import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {

    @POST("/send")
    suspend fun sendMessage(
        @Body body : SendMessageDto
    )

    @POST("/broadcast")
    suspend fun broadcast(
        @Body body : SendMessageDto
    )
}