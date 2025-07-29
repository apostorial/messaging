package ma.tayeb.messaging_backend.services.implementations.conversation;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.exceptions.EntityNotFoundException;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;
import ma.tayeb.messaging_backend.services.interfaces.conversation.ConversationInternalService;

@Service
@RequiredArgsConstructor
public class ConversationInternalServiceImpl implements ConversationInternalService {
    private final ConversationRepository conversationRepository;

    @Override
    public Conversation findById(UUID conversationId) {
        return conversationRepository.findById(conversationId).orElseThrow(() -> new EntityNotFoundException("Conversation with id " + conversationId + " not found."));
    }

    @Override
    public Conversation updateTime(Conversation conversation) {
        conversation.setLastUpdated(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }
}
