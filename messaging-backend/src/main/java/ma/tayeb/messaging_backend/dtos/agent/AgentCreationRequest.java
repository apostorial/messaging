package ma.tayeb.messaging_backend.dtos.agent;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentCreationRequest {
    private String fullName;
    private String email;
}
