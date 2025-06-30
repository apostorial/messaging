package ma.tayeb.messaging_backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Conversation;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Conversation findByCustomer(String customerId);
}
