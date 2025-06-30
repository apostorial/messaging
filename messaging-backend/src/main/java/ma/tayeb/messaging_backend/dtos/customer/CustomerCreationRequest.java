package ma.tayeb.messaging_backend.dtos.customer;

import lombok.Getter;

@Getter
public class CustomerCreationRequest {
    private String prospectId;
    private String clientId;
    private String fullName;
}
