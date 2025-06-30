package ma.tayeb.messaging_backend.dtos.conversation;

import lombok.Builder;
import lombok.Getter;
import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;

@Getter @Builder
public class ConversationCreationRequest {
    private CustomerCreationRequest customer;
}
