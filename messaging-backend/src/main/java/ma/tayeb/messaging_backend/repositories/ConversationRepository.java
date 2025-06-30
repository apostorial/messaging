package ma.tayeb.messaging_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
} 