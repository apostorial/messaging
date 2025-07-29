package ma.tayeb.messaging_backend.services.implementations;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.exceptions.EntityNotFoundException;
import ma.tayeb.messaging_backend.mappers.CustomerMapper;
import ma.tayeb.messaging_backend.repositories.CustomerRepository;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;
import ma.tayeb.messaging_backend.services.interfaces.conversation.ConversationService;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    private final ConversationService conversationService;

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
            Customer savedCustomer = customerRepository.save(customer);
            Conversation conversation = conversationService.create(savedCustomer);
            savedCustomer.setConversation(conversation);
            return customerRepository.save(savedCustomer);
        } else {
            return customer;
        }
    }

    @Override
    public Customer findByProspectId(String prospectId) {
        return customerRepository.findByProspectId(prospectId);
    }

    @Override
    public Customer findByClientId(String clientId) {
        return customerRepository.findByClientId(clientId);
    }

    @Override
    public Customer findById(UUID customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new EntityNotFoundException("Customer with id " + customerId + " not found."));
    }
}
