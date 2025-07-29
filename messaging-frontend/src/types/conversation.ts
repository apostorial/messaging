import type { Customer } from "./customer"

export type Conversation = {
    id: string,
    owner: Customer,
    lastUpdated: Date
}

export type ConversationResponse = {
    id: string,
    owner: Customer,
    lastUpdated: Date,
    lastMessageSender: string,
    lastMessageContent: string,
    unreadCount: number
}