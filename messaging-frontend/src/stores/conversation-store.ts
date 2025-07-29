import { create } from "zustand";
import type { ConversationResponse } from "../types/conversation";

type ConversationStore = {
  conversations: ConversationResponse[]
  setConversations: (conversations: ConversationResponse[]) => void
  appendConversations: (conversations: ConversationResponse[]) => void
}

export const useConversationStore = create<ConversationStore>((set) => ({
  conversations: [],
  setConversations: (conversations => set({conversations})),
  appendConversations: (conversations => set(state => ({ 
    conversations: [...state.conversations, ...conversations] 
  })))
}))