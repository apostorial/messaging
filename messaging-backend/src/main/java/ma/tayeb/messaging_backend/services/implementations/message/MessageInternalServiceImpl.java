package ma.tayeb.messaging_backend.services.implementations.message;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.SenderType;
import ma.tayeb.messaging_backend.exceptions.EntityNotFoundException;
import ma.tayeb.messaging_backend.repositories.MessageRepository;
import ma.tayeb.messaging_backend.services.interfaces.message.MessageInternalService;

@Service
@RequiredArgsConstructor
public class MessageInternalServiceImpl implements MessageInternalService {
    private final MessageRepository messageRepository;

    @Override
    public Message findById(UUID messageId) {
        return messageRepository.findById(messageId).orElseThrow(() -> new EntityNotFoundException("Message not found with ID: `" + messageId + "`"));
    }

    @Override
    public Page<Message> findAllByConversation(UUID conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByConversation_IdOrderByTimestampAsc(conversationId, pageable);
    }

    @Override
    public Message findTopMessageByConversation(UUID conversationId) {
        return messageRepository.findTopByConversation_IdOrderByTimestampDesc(conversationId);
    }

    @Override
    public List<Message> findUnreadMessagesByConversation(UUID conversationId) {
        return messageRepository.findByConversation_IdAndReadFalse(conversationId);
    }

    @Override
    public List<Message> findUnreadMessagesBySenderByConversation(UUID conversationId, SenderType senderType) {
        return messageRepository.findByConversation_IdAndSenderTypeAndReadFalse(conversationId, senderType);
    }

    @Override
    public int unreadCountBySenderByConversation(UUID conversationId, SenderType senderType) {
        return messageRepository.countByConversation_IdAndSenderTypeAndReadFalse(conversationId, senderType);
    }
}
