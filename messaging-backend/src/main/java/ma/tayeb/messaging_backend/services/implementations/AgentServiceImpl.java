package ma.tayeb.messaging_backend.services.implementations;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.agent.AgentCreationRequest;
import ma.tayeb.messaging_backend.entities.Agent;
import ma.tayeb.messaging_backend.exceptions.EntityNotFoundException;
import ma.tayeb.messaging_backend.mappers.AgentMapper;
import ma.tayeb.messaging_backend.repositories.AgentRepository;
import ma.tayeb.messaging_backend.services.interfaces.AgentService;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    private final AgentRepository agentRepository;
    private final AgentMapper agentMapper;

    @Override
    public Agent findOrCreate(AgentCreationRequest request) {
        if (request.getEmail() != null) {
            Agent existing = agentRepository.findByEmail(request.getEmail());
            if (existing != null)
                return existing;
        }

        Agent agent = agentMapper.fromCreationRequestToEntity(request);

        try {
            return agentRepository.save(agent);
        } catch (DataIntegrityViolationException e) {
            return agentRepository.findByEmail(request.getEmail());
        }
    }

    @Override
    public Agent findByEmail(String email) {
        return agentRepository.findByEmail(email);
    }

    @Override
    public Agent findById(UUID agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent with id " + agentId + " not found."));
    }
}
