package ma.tayeb.messaging_backend.services.implementations;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.tayeb.messaging_backend.dtos.conversation.ConversationCreationRequest;
import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.dtos.message.MessageUpdateRequest;
import ma.tayeb.messaging_backend.entities.Agent;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.exceptions.EntityNotFoundException;
import ma.tayeb.messaging_backend.mappers.MessageMapper;
import ma.tayeb.messaging_backend.repositories.MessageRepository;
import ma.tayeb.messaging_backend.services.interfaces.AgentService;
import ma.tayeb.messaging_backend.services.interfaces.ConversationService;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;
import ma.tayeb.messaging_backend.services.interfaces.MessageService;

@Service @RequiredArgsConstructor @Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    private final CustomerService customerService;
    private final AgentService agentService;
    private final ConversationService conversationService;

    @Override
    public Message create(MessageCreationRequest request) {
        Customer customer = null;
        Agent agent = null;
        Conversation conversation;

        Message message = messageMapper.fromCreationRequestToEntity(request);
        if(!ObjectUtils.isEmpty(request.getCustomer())) {
            customer = customerService.findOrCreate(request.getCustomer());
        }

        if(!ObjectUtils.isEmpty(request.getAgent())) {
            agent = agentService.findOrCreate(request.getAgent());
        }

        if (request.getConversationId() == null) {
            ConversationCreationRequest conversationCreationRequest = ConversationCreationRequest.builder()
                .customer(request.getCustomer())
                .build();
            conversation = conversationService.create(conversationCreationRequest);
        } else {
            conversation = conversationService.findById(request.getConversationId());
        }

        message.setCustomer(customer);
        message.setAgent(agent);
        message.setConversation(conversation);
        return messageRepository.save(message);
    }

    @Override
    public Message findById(String messageId) {
        return messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message with ID '" + messageId + "' not found."));
    }

    @Override
    public Page<Message> findAllByConversation(String conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findByConversationId(conversationId, pageable);
    }

    @Override
    public Message update(String messageId, MessageUpdateRequest request) {
        Message message = findById(messageId);
        log.error(message.getConversation().toString());
        message.setContent(request.getContent());
        message.setUpdatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    @Override
    public Message reply(String messageId, MessageCreationRequest request) {
        Message newMessage = create(request);
        Message repliedToMessage = findById(messageId);
        newMessage.setRepliedTo(repliedToMessage);
        return messageRepository.save(newMessage);
    }
    
}
