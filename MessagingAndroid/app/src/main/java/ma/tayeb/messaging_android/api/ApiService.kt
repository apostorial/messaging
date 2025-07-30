package ma.tayeb.messaging_android.api

import ma.tayeb.messaging_android.enums.ReaderType
import ma.tayeb.messaging_android.enums.SenderType
import ma.tayeb.messaging_android.types.Customer
import ma.tayeb.messaging_android.types.CustomerCreationRequest
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.types.MessageCreationRequest
import ma.tayeb.messaging_android.types.PaginatedResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface ApiService {
    @POST("/api/customers/find-or-create")
    suspend fun findOrCreate(@Body request: CustomerCreationRequest): Customer

    @PATCH("/api/messages/conversation/{conversationId}")
    suspend fun markAsRead(@Path("conversationId") conversationId: UUID, @Query("readerType") readerType: ReaderType): Response<Void>

    @GET("/api/messages/conversation/{conversationId}")
    suspend fun findAllMessagesByConversation(@Path("conversationId") conversationId: UUID, @Query("page") page: Int, @Query("size") size: Int): PaginatedResponse<Message>

    @Multipart
    @POST("/api/messages/send")
    suspend fun sendMessage(
        @PartMap formData: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part? = null
    ): Response<Void>

    @PATCH("/api/messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId: String,
        @Query("content") content: String
    ): Response<Void>


}