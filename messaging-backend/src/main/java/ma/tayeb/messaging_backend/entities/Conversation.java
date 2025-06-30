package ma.tayeb.messaging_backend.entities;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Conversation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private Customer owner;
} 