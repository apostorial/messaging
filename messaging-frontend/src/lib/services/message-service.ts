import api from "../axios";
import type { PaginatedResponse } from "../../types/pagination";
import type { Message, MessageCreationRequest } from "../../types/message";

export async function findAll(conversationId: string, page: number = 0, size: number = 10): Promise<PaginatedResponse<Message>> {

    const response = await api.get<PaginatedResponse<Message>>(
        `/api/messages/conversation/${conversationId}?page=${page}&size=${size}`
    );

    return response.data;
}

export async function send(request: MessageCreationRequest, file?: File): Promise<void> {
    const formData = new FormData();
    formData.append('content', request.content);
    formData.append('agentId', request.agentId);
    formData.append('senderType', request.senderType);
    formData.append('conversationId', request.conversationId);
    if (request.replyToId) {
        formData.append('replyToId', request.replyToId);
    }
    if (file) {
        formData.append('file', file);
    }

    await api.post('/api/messages/send', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
}

export async function markAsRead(conversationId: string): Promise<void> {
    await api.patch(`/api/messages/conversation/${conversationId}?readerType=AGENT`);
}

export async function editMessage(messageId: string, content: string): Promise<void> {
    await api.patch(`/api/messages/${messageId}?content=${encodeURIComponent(content)}`);
}