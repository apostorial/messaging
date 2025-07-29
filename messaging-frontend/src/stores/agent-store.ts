import { create } from "zustand";
import type { Agent } from "../types/agent";

type AgentStore = {
  agent: Agent | null
  setAgent: (agent: Agent) => void
}

export const useAgentStore = create<AgentStore>((set) => ({
  agent: null,
  setAgent: (agent => set({agent}))
}))