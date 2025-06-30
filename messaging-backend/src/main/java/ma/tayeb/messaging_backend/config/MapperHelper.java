package ma.tayeb.messaging_backend.config;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;

@Component @RequiredArgsConstructor
public class MapperHelper {
    private final CustomerService customerService;

    public Customer mapCustomer(CustomerCreationRequest request) {
        return customerService.findOrCreate(request);
    }
    
}
