import api from "../axios";
import type { ConversationResponse } from "../../types/conversation";
import type { PaginatedResponse } from "../../types/pagination";

export async function findAll(page: number = 0, size: number = 10): Promise<PaginatedResponse<ConversationResponse>> {

    const response = await api.get<PaginatedResponse<ConversationResponse>>(
        `/api/conversations?page=${page}&size=${size}`
    );

    return response.data;
}