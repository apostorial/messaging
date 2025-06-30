package ma.tayeb.messaging_backend.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.entities.Message;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "conversation", ignore = true)
    @Mapping(target = "repliedTo", ignore = true)
    Message fromCreationRequestToEntity(MessageCreationRequest request);
}
