package ma.tayeb.messaging_backend.services.interfaces;

import ma.tayeb.messaging_backend.dtos.agent.AgentCreationRequest;
import ma.tayeb.messaging_backend.entities.Agent;

public interface AgentService {
    Agent findOrCreate(AgentCreationRequest request);
}
