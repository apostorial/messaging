package ma.tayeb.messaging_android.types

import java.util.UUID

data class Customer(
    val id: UUID,
    val fullName: String,
    val prospectId: String,
    val clientId: String,
    val conversation: Conversation
)

data class CustomerCreationRequest(
    val fullName: String,
    val clientId: String?,
    val prospectId: String?
)