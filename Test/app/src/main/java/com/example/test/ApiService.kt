package com.example.test

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/customers/find-or-create")
    suspend fun findOrCreate(@Body request: CustomerCreationRequest): Customer
}