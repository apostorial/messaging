package ma.tayeb.messaging_backend.services.implementations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.conversation.ConversationCreationRequest;
import ma.tayeb.messaging_backend.dtos.customer.CustomerIdentifierRequest;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.exceptions.EntityNotFoundException;
import ma.tayeb.messaging_backend.mappers.ConversationMapper;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;
import ma.tayeb.messaging_backend.services.interfaces.ConversationService;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;

@Service @RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMapper conversationMapper;

    private final CustomerService customerService;

    @Override
    public Conversation create(ConversationCreationRequest request) {
        Conversation conversation = conversationMapper.fromCreationRequestToEntity(request);
        return conversationRepository.save(conversation);
    }
    
    @Override
    public Conversation findById(String conversationId) {
        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> new EntityNotFoundException("Conversation with ID '" + conversationId + "' not found."));
    }

    @Override
    public Conversation findByCustomer(CustomerIdentifierRequest request) {
        Customer customer = null;

        if (request.getProspectId() != null) {
            customer = customerService.findByProspectId(request.getProspectId());
        } else if (request.getClientId() != null) {
            customer = customerService.findByClientId(request.getClientId());
        }

        if (customer == null) {
            throw new EntityNotFoundException("No conversation was found for this customer.");
        }

        // Conversation conversation = conversationRepository.findByCustomer(customer.getId());
        return conversationRepository.findByCustomer(customer.getId());
    }
    
    @Override
    public Page<Conversation> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return conversationRepository.findAll(pageable);
    }
}
