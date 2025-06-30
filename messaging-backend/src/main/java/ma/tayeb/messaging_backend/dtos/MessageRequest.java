package ma.tayeb.messaging_backend.dtos;

import java.util.UUID;
import lombok.Data;

@Data
public class MessageRequest {
    private String content;
    private UUID senderId;
    private UUID conversationId;
} 