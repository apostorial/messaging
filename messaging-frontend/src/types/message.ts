import type { Agent } from "./agent"
import type { Conversation } from "./conversation"
import type { Customer } from "./customer"

export type Message = {
    id: string,
    content: string,
    timestamp: Date,
    replyTo: Message,
    fileUrl: string,
    customer: Customer,
    agent: Agent,
    edited: boolean,
    read: boolean,
    senderType: 'CUSTOMER' | 'AGENT',
    conversation: Conversation
}

export type MessageCreationRequest = {
    content: string,
    agentId: string,
    senderType: 'CUSTOMER' | 'AGENT',
    conversationId: string,
    replyToId?: string
}