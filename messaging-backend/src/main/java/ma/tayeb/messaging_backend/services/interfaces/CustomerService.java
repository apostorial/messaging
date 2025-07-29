package ma.tayeb.messaging_backend.services.interfaces;

import java.util.UUID;

import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Customer;

public interface CustomerService {
    Customer findOrCreate(CustomerCreationRequest request);
    Customer findByProspectId(String prospectId);
    Customer findByClientId(String clientId);
    Customer findById(UUID customerId);
}
