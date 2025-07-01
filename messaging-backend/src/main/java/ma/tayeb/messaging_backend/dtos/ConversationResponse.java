package ma.tayeb.messaging_backend.dtos;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Builder;
import ma.tayeb.messaging_backend.entities.Customer;

@Getter @Builder
public class ConversationResponse {
    private UUID id;
    private Customer customer;
    private LocalDateTime lastMessageTime;
    private String lastMessageContent;
    private int unreadCount;
} 