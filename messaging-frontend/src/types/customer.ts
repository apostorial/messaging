import type { Conversation } from "./conversation"

export type Customer = {
    id: string,
    fullName: string,
    prospectId: string,
    clientId: string,
    conversation: Conversation
}