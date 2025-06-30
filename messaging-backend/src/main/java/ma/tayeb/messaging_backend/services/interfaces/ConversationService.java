package ma.tayeb.messaging_backend.services.interfaces;

import org.springframework.data.domain.Page;

import ma.tayeb.messaging_backend.dtos.conversation.ConversationCreationRequest;
import ma.tayeb.messaging_backend.dtos.customer.CustomerIdentifierRequest;
import ma.tayeb.messaging_backend.entities.Conversation;

public interface ConversationService {
    Conversation create(ConversationCreationRequest request);
    Conversation findById(String conversationId);
    Conversation findByCustomer(CustomerIdentifierRequest request);
    Page<Conversation> findAll(int page, int size);
}
