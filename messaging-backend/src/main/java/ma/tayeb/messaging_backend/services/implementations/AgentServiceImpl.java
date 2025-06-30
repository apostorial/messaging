package ma.tayeb.messaging_backend.services.implementations;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.agent.AgentCreationRequest;
import ma.tayeb.messaging_backend.entities.Agent;
import ma.tayeb.messaging_backend.mappers.AgentMapper;
import ma.tayeb.messaging_backend.repositories.AgentRepository;
import ma.tayeb.messaging_backend.services.interfaces.AgentService;

@Service @RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    private final AgentRepository agentRepository;
    private final AgentMapper agentMapper;
    
    @Override
    public Agent findOrCreate(AgentCreationRequest request) {
        Agent agent = null;

        if (request.getEmail() != null) {
            agent = agentRepository.findByEmail(request.getEmail());
        }

        if (agent == null) {
            agent = agentMapper.fromCreationRequestToEntity(request);
            return agentRepository.save(agent);
        } else {
            return agent;
        }
    }
}
