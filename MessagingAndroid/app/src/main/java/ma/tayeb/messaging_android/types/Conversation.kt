package ma.tayeb.messaging_android.types

import org.threeten.bp.LocalDateTime
import java.util.UUID

data class Conversation(
    val id: UUID,
    val owner: Customer,
    val lastUpdated: LocalDateTime
)

data class ConversationResponse(
    val id: UUID,
    val owner: Customer,
    val lastUpdated: LocalDateTime,
    val lastMessageSender: String,
    val lastMessageContent: String,
    val unreadCount: Int
)