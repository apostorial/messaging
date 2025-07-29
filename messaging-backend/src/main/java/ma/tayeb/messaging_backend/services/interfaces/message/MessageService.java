package ma.tayeb.messaging_backend.services.interfaces.message;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.ReaderType;

public interface MessageService {
    void send(MessageCreationRequest request, MultipartFile file);
    Page<Message> findAllByConversation(UUID conversationId, int page, int size);
    void edit(UUID messageId, String content);
    void markAsRead(UUID conversationId, ReaderType readerType);
}
