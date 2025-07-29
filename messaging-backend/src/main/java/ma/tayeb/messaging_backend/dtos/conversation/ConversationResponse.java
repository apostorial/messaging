package ma.tayeb.messaging_backend.dtos.conversation;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import ma.tayeb.messaging_backend.entities.Customer;

@Getter
@Builder
public class ConversationResponse {
    private UUID id;
    private Customer owner;
    private LocalDateTime lastUpdated;
    private String lastMessageSender;
    private String lastMessageContent;
    private int unreadCount;
}
