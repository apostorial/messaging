package ma.tayeb.messaging_backend.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.tayeb.messaging_backend.entities.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId);
} 