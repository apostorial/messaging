package ma.tayeb.messaging_backend.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;

import java.util.List;
import ma.tayeb.messaging_backend.entities.Conversation;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId, Pageable pageable);
    Message findTopByConversationOrderByTimestampDesc(Conversation conversation);
    List<Message> findByConversationIdAndReadFalse(UUID conversationId);
    List<Message> findByConversationIdAndSenderTypeAndReadFalse(UUID conversationId, SenderType senderType);
} 