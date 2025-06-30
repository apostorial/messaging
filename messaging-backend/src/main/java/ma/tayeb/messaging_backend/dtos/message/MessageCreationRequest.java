package ma.tayeb.messaging_backend.dtos.message;

import lombok.Getter;
import ma.tayeb.messaging_backend.dtos.agent.AgentCreationRequest;
import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;

@Getter
public class MessageCreationRequest {
    private String content;
    private String attachmentId;
    private CustomerCreationRequest customer;
    private AgentCreationRequest agent;
    private String conversationId;
}
