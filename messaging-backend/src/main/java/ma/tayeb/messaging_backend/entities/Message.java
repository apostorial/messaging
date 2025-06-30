package ma.tayeb.messaging_backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.tayeb.messaging_backend.enums.MessageType;
import ma.tayeb.messaging_backend.enums.SenderType;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Message {
    @Id
    private UUID id;
    private String content;
    private LocalDateTime timestamp;
    
    private UUID senderId;
    @Enumerated(EnumType.STRING)
    private SenderType senderType;
    @Enumerated(EnumType.STRING)
    private MessageType type;
    private String fileUrl;

    @ManyToOne
    private Conversation conversation;
}
