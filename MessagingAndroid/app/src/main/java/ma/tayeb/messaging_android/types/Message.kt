package ma.tayeb.messaging_android.types

import ma.tayeb.messaging_android.enums.SenderType
import org.threeten.bp.LocalDateTime
import java.util.UUID

data class Message(
    val id: String?,
    var content: String?,
//    val timestamp: LocalDateTime,
    val timestamp: String,
    val replyTo: Message?,
    val fileUrl: String?,
    val customer: Customer?,
    val agent: Agent?,
    val edited: Boolean,
    val read: Boolean,
    val senderType: SenderType,
    val conversation: Conversation
)

data class MessageCreationRequest(
    val content: String?,
    val customerId: UUID,
    val senderType: SenderType,
    val conversationId: UUID,
    val replyToId: String?
)