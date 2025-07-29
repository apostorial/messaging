package ma.tayeb.messaging_backend.dtos.customer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerCreationRequest {
    private String fullName;
    private String prospectId;
    private String clientId;
}
