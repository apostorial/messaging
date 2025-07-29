package ma.tayeb.messaging_android.api

import ma.tayeb.messaging_android.types.Customer
import ma.tayeb.messaging_android.types.CustomerCreationRequest
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.types.PaginatedResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface ApiService {
    @POST("/api/customers/find-or-create")
    suspend fun findOrCreate(@Body request: CustomerCreationRequest): Customer

    @GET("/api/messages/conversation/{conversationId}")
    suspend fun getMessagesByConversationId(
        @Path("conversationId") conversationId: UUID,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PaginatedResponse<Message>
}