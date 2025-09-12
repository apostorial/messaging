import { create } from "zustand";
import type { Message } from "../types/message";

type MessageStore = {
  messages: Message[]
  setMessages: (messages: Message[]) => void
  appendMessages: (messages: Message[]) => void
  clearMessages: () => void
  markMessagesAsRead: (messageIds: string[]) => void
  editMessage: (messageId: string, content: string) => void
  upsertMessage: (message: Message) => void
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
  })),
  upsertMessage: (message) => set(state => {
    const existingIndex = state.messages.findIndex(m => m.id === message.id);
    if (existingIndex !== -1) {
      const updated = [...state.messages];
      updated[existingIndex] = message;
      return { messages: updated };
    }
    return { messages: [...state.messages, message] };
  })
})) 