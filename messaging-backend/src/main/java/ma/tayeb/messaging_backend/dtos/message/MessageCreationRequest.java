package ma.tayeb.messaging_backend.dtos.message;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import ma.tayeb.messaging_backend.enums.SenderType;

@Getter
@Builder
public class MessageCreationRequest {
    private String content;
    private UUID customerId;
    private UUID agentId;
    private SenderType senderType;
    private UUID conversationId;
    private UUID replyToId;
}
