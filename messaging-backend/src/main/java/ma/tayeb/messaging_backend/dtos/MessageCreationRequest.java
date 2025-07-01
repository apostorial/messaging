package ma.tayeb.messaging_backend.dtos;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MessageCreationRequest {
    private String content;
    private UUID senderId;
    private UUID conversationId;
    private UUID replyToId;
} 