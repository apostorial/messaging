package ma.tayeb.messaging_backend.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Customer {
    @Id
    private String id;

    @Indexed(unique = true)
    private String prospectId;

    @Indexed(unique = true)
    private String clientId;

    private String fullName;
}
