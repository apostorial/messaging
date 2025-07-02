package ma.tayeb.messaging_backend.services.implementations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.ConversationResponse;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;
import ma.tayeb.messaging_backend.repositories.MessageRepository;
import ma.tayeb.messaging_backend.services.interfaces.ConversationService;

@Service @RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    @Override
    public Page<ConversationResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = conversationRepository.findAllByOrderByLastUpdatedDesc(pageable);
        
        return conversations.map(conv -> {
            Message lastMessage = messageRepository.findTopByConversationOrderByTimestampDesc(conv);
            int unreadCount = messageRepository
                    .findByConversationIdAndSenderTypeAndReadFalse(conv.getId(), SenderType.CUSTOMER)
                    .size();

            return ConversationResponse.builder()
                    .id(conv.getId())
                    .customer(conv.getOwner())
                    .lastMessageTime(lastMessage != null ? lastMessage.getTimestamp() : null)
                    .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
                    .unreadCount(unreadCount)
                    .build();
        });
    }
}
