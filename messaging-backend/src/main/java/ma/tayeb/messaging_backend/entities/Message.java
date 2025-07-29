package ma.tayeb.messaging_backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.tayeb.messaging_backend.enums.SenderType;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    private String content;

    private LocalDateTime timestamp;

    @ManyToOne
    private Message replyTo;

    private String fileUrl;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Agent agent;

    private boolean edited;
    private boolean read;

    @Enumerated(EnumType.STRING)
    private SenderType senderType;

    @ManyToOne
    private Conversation conversation;

    @PrePersist
    public void PrePersist() {
        timestamp = LocalDateTime.now();
        edited = false;
        read = false;
    }
}
