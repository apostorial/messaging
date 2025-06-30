package ma.tayeb.messaging_backend.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Customer;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "id", ignore = true)
    Customer fromCreationRequestToEntity(CustomerCreationRequest request);
}
