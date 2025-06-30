package ma.tayeb.messaging_backend.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Message {
    @Id
    private String id;

    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @DBRef
    private Message repliedTo;

    private String attachmentId;

    @DBRef
    private Customer customer;

    @DBRef
    private Agent agent;

    @DBRef
    private Conversation conversation;

    // @JsonProperty("repliedTo")
    // public String getRepliedToForSerialization() {
    //     return repliedTo != null ? repliedTo.getId() : null;
    // }

    @JsonProperty("conversation")
    public String getConversationForSerialization() {
        return conversation != null ? conversation.getId() : null;
    }
}
