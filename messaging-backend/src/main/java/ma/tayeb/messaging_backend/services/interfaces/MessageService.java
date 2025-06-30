package ma.tayeb.messaging_backend.services.interfaces;

import org.springframework.data.domain.Page;

import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.dtos.message.MessageUpdateRequest;
import ma.tayeb.messaging_backend.entities.Message;

public interface MessageService {
    Message create(MessageCreationRequest request);
    Message findById(String messageId);
    Page<Message> findAllByConversation(String conversationId, int page, int size);
    Message update(String messageId, MessageUpdateRequest request);
    Message reply(String messageId, MessageCreationRequest request);
}
