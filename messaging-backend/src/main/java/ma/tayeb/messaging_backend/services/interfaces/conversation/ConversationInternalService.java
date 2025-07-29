package ma.tayeb.messaging_backend.services.interfaces.conversation;

import java.util.UUID;

import ma.tayeb.messaging_backend.entities.Conversation;

public interface ConversationInternalService {
    Conversation findById(UUID conversationId);
    Conversation updateTime(Conversation conversation);
}
