package ma.tayeb.messaging_backend.services.implementations;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.mappers.CustomerMapper;
import ma.tayeb.messaging_backend.repositories.CustomerRepository;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;

@Service @RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Customer findByProspectId(String prospectId) {
        return customerRepository.findByProspectId(prospectId);
    }

    @Override
    public Customer findByClientId(String clientId) {
        return customerRepository.findByClientId(clientId);
    }

    @Override
    public Customer findOrCreate(CustomerCreationRequest request) {
        Customer customer = null;

        if (request.getProspectId() != null) {
            customer = findByProspectId(request.getProspectId());
        } else if (request.getClientId() != null) {
            customer = findByClientId(request.getClientId());
        }

        if (customer == null) {
            customer = customerMapper.fromCreationRequestToEntity(request);
            return customerRepository.save(customer);
        } else {
            return customer;
        }
    }
}
