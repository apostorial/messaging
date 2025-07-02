package ma.tayeb.messaging_backend.services.interfaces;

import org.springframework.data.domain.Page;

import ma.tayeb.messaging_backend.dtos.ConversationResponse;

public interface ConversationService {
    Page<ConversationResponse> findAll(int page, int size);
}
