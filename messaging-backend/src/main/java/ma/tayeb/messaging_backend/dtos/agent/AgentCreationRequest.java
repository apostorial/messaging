package ma.tayeb.messaging_backend.dtos.agent;

import lombok.Getter;

@Getter
public class AgentCreationRequest {
    private String email;
    private String fullName;
}
