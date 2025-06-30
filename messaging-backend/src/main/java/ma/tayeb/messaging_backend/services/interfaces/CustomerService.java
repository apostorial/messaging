package ma.tayeb.messaging_backend.services.interfaces;

import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Customer;

public interface CustomerService {
    Customer findByProspectId(String prospectId);
    Customer findByClientId(String clientId);
    Customer findOrCreate(CustomerCreationRequest request);
}
