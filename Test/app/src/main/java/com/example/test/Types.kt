package com.example.test

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

data class Conversation(
    val id: UUID,
    val owner: Customer,
    val lastUpdated: String
)

data class Message(
    val id: String?,
    val content: String?,
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

data class Agent(
    val id: String,
    val email: String,
    val fullName: String
)

enum class SenderType {
    AGENT,
    CUSTOMER
}