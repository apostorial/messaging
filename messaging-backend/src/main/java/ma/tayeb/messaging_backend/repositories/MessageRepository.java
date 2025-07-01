package ma.tayeb.messaging_backend.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;

import java.util.List;
import ma.tayeb.messaging_backend.entities.Conversation;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId);
    Message findTopByConversationOrderByTimestampDesc(Conversation conversation);
    List<Message> findByConversationIdAndReadFalse(UUID conversationId);
    List<Message> findByConversationIdAndSenderTypeAndReadFalse(UUID conversationId, SenderType senderType);
} 