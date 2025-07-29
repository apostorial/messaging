package ma.tayeb.messaging_backend.services.interfaces.conversation;

import org.springframework.data.domain.Page;

import ma.tayeb.messaging_backend.dtos.conversation.ConversationResponse;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Customer;

public interface ConversationService {
    Conversation create(Customer customer);
    Page<ConversationResponse> findAll(int page, int size);
}
