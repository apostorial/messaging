package ma.tayeb.messaging_backend.entities;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    
    @Id
    @GeneratedValue
    private UUID id;

    private String fullName;
    private String prospectId;
    private String clientId;

    @OneToOne
    @JsonIncludeProperties({"id"})
    private Conversation conversation;
}
