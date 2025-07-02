package ma.tayeb.messaging_backend.dtos;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageFileCreationRequest {
    private String content;
    private UUID senderId;
    private UUID conversationId;
    private UUID replyToId;
    private MultipartFile file;
}
