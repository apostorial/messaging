package ma.tayeb.messaging_backend.dtos.customer;

import lombok.Getter;

@Getter
public class CustomerIdentifierRequest {
    private String prospectId;
    private String clientId;
}
