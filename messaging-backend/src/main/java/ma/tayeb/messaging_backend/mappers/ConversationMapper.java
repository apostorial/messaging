package ma.tayeb.messaging_backend.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ma.tayeb.messaging_backend.config.MapperHelper;
import ma.tayeb.messaging_backend.dtos.conversation.ConversationCreationRequest;
import ma.tayeb.messaging_backend.entities.Conversation;

@Mapper(componentModel = "spring", uses = MapperHelper.class)
public interface ConversationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Conversation fromCreationRequestToEntity(ConversationCreationRequest request);
}
