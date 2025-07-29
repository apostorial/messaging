package ma.tayeb.messaging_backend.services.implementations.conversation;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.conversation.ConversationResponse;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;
import ma.tayeb.messaging_backend.services.interfaces.conversation.ConversationService;
import ma.tayeb.messaging_backend.services.interfaces.message.MessageInternalService;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;

    private final MessageInternalService messageInternalService;

    @Override
    public Conversation create(Customer customer) {
        Conversation conversation = Conversation.builder()
                .owner(customer)
                .lastUpdated(LocalDateTime.of(1970, 1, 1, 0, 0))
                .build();

        return conversationRepository.save(conversation);
    }

    @Override
    public Page<ConversationResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = conversationRepository.findAllByOrderByLastUpdatedDesc(pageable);

        return conversations.map(conversation -> {
            Message lastMessage = messageInternalService.findTopMessageByConversation(conversation.getId());
            int unreadCount = messageInternalService.unreadCountBySenderByConversation(conversation.getId(),
                    SenderType.CUSTOMER);

            ConversationResponse.ConversationResponseBuilder builder = ConversationResponse.builder()
                    .id(conversation.getId())
                    .owner(conversation.getOwner())
                    .lastUpdated(conversation.getLastUpdated())
                    .unreadCount(unreadCount);

            if (lastMessage != null) {
                if (lastMessage.getSenderType() != null && lastMessage.getSenderType() == SenderType.CUSTOMER) {
                    builder.lastMessageSender(lastMessage.getCustomer().getFullName());
                } else if (lastMessage.getSenderType() != null && lastMessage.getSenderType() == SenderType.AGENT) {
                    builder.lastMessageSender(lastMessage.getAgent().getFullName());
                }

                if (StringUtils.hasText(lastMessage.getContent())) {
                    builder.lastMessageContent(lastMessage.getContent());
                } else if (lastMessage.getFileUrl() != null) {
                    builder.lastMessageContent("a envoyé une pièce jointe");
                }
            }

            return builder.build();
        });
    }
}