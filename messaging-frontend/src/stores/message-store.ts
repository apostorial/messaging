import { create } from "zustand";
import type { Message } from "../types/message";

type MessageStore = {
  messages: Message[]
  setMessages: (messages: Message[]) => void
  appendMessages: (messages: Message[]) => void
  clearMessages: () => void
  markMessagesAsRead: (messageIds: string[]) => void
  editMessage: (messageId: string, content: string) => void
}

export const useMessageStore = create<MessageStore>((set) => ({
  messages: [],
  setMessages: (messages => set({messages})),
  appendMessages: (messages => set(state => ({ 
    messages: [...state.messages, ...messages] 
  }))),
  clearMessages: () => set({ messages: [] }),
  markMessagesAsRead: (messageIds => set(state => ({
    messages: state.messages.map(message => 
      messageIds.includes(message.id) ? { ...message, read: true } : message
    )
  }))),
  editMessage: (messageId, content) => set(state => ({
    messages: state.messages.map(message => 
      message.id === messageId ? { ...message, content, edited: true } : message
    )
  }))
})) 