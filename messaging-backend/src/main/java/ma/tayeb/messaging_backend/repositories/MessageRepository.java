package ma.tayeb.messaging_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;


@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversation_IdOrderByTimestampAsc(UUID conversationId, Pageable pageable);
    Message findTopByConversation_IdOrderByTimestampDesc(UUID conversationId);
    List<Message> findByConversation_IdAndReadFalse(UUID conversationId);
    List<Message> findByConversation_IdAndSenderTypeAndReadFalse(UUID conversationId, SenderType senderType);
    int countByConversation_IdAndSenderTypeAndReadFalse(UUID conversationId, SenderType senderType);
}
