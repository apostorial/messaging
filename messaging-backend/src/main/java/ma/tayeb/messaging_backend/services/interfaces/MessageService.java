package ma.tayeb.messaging_backend.services.interfaces;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import ma.tayeb.messaging_backend.dtos.MessageCreationRequest;
import ma.tayeb.messaging_backend.dtos.MessageEditRequest;
import ma.tayeb.messaging_backend.dtos.MessageReadRequest;
import ma.tayeb.messaging_backend.entities.Message;

public interface MessageService {
    void send(MessageCreationRequest request);
    void uploadAndSend(MultipartFile file, MessageCreationRequest request);
    Page<Message> findAllByConversation(UUID conversationId, int page, int size);
    void edit(MessageEditRequest request);
    void markAsRead(MessageReadRequest request);
}
