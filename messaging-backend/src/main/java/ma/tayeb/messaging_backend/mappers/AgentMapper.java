package ma.tayeb.messaging_backend.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ma.tayeb.messaging_backend.dtos.agent.AgentCreationRequest;
import ma.tayeb.messaging_backend.entities.Agent;

@Mapper(componentModel = "spring")
public interface AgentMapper {
    @Mapping(target = "id", ignore = true)
    Agent fromCreationRequestToEntity(AgentCreationRequest request);
}
