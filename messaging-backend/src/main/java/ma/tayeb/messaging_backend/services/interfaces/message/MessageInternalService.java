package ma.tayeb.messaging_backend.services.interfaces.message;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;

public interface MessageInternalService {
    Message findById(UUID messageId);
    Page<Message> findAllByConversation(UUID conversationId, int page, int size);
    Message findTopMessageByConversation(UUID conversationId);
    List<Message> findUnreadMessagesByConversation(UUID conversationId);
    List<Message> findUnreadMessagesBySenderByConversation(UUID conversationId, SenderType senderType);
    int unreadCountBySenderByConversation(UUID conversationId, SenderType senderType);
}
