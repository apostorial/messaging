package ma.tayeb.messaging_backend.dtos.conversation;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Getter;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.entities.Message;

@Getter @Builder
public class ConversationLoadedResponse {
    private String id;
    private Customer customer;
    private Page<Message> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
