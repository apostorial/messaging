package com.example.test

import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface ApiService {
    @POST("/api/customers/find-or-create")
    suspend fun findOrCreate(@Body request: CustomerCreationRequest): Customer

    @PATCH("/api/messages/conversation/{conversationId}")
    suspend fun markAsRead(@Path("conversationId") conversationId: UUID, @Query("readerType") readerType: reader)
}