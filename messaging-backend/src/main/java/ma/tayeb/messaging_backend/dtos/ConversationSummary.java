package ma.tayeb.messaging_backend.dtos;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummary {
    private UUID id;
    private String customerName;
    private LocalDateTime lastMessageTime;
    private String lastMessageContent;
    private int unreadCount;
} 