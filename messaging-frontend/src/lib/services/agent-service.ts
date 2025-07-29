import api from "../axios";
import type { Agent, AgentCreationRequest } from "../../types/agent";

export async function findOrCreate(
    payload: AgentCreationRequest
): Promise<Agent> {

    const response = await api.post<Agent>(
        "/api/agents/find-or-create",
        payload
    );

    return response.data;
}