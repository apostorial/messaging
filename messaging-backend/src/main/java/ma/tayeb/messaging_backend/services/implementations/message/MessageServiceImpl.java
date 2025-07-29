package ma.tayeb.messaging_backend.services.implementations.message;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.entities.Agent;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.ReaderType;
import ma.tayeb.messaging_backend.enums.SenderType;
import ma.tayeb.messaging_backend.repositories.MessageRepository;
import ma.tayeb.messaging_backend.services.interfaces.AgentService;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;
import ma.tayeb.messaging_backend.services.interfaces.FileService;
import ma.tayeb.messaging_backend.services.interfaces.conversation.ConversationInternalService;
import ma.tayeb.messaging_backend.services.interfaces.message.MessageInternalService;
import ma.tayeb.messaging_backend.services.interfaces.message.MessageService;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final MessageInternalService messageInternalService;

    private final CustomerService customerService;
    private final AgentService agentService;
    private final FileService fileService;
    private final ConversationInternalService conversationInternalService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void send(MessageCreationRequest request, MultipartFile file) {
        Message replyTo = null;
        String fileUrl = null;

        if (request.getReplyToId() != null) {
            replyTo = messageInternalService.findById(request.getReplyToId());   
        }

        Customer customer = null;
        Agent agent = null;

        if (request.getSenderType() == SenderType.CUSTOMER) {
            customer = customerService.findById(request.getCustomerId());
        } else if (request.getSenderType() == SenderType.AGENT) {
            agent = agentService.findById(request.getAgentId());
        }

        Conversation conversation = conversationInternalService.findById(request.getConversationId());

        if (file != null) {
            fileUrl = fileService.upload(file);
        }

        Message.MessageBuilder builder = Message.builder()
            .content(request.getContent())
            .senderType(request.getSenderType())
            .conversation(conversation);
            
        if (customer != null) builder.customer(customer);
        if (agent != null) builder.agent(agent);
        if (replyTo != null) builder.replyTo(replyTo);
        if (fileUrl != null) builder.fileUrl(fileUrl);


        Message message = builder.build();
        Message savedMessage = messageRepository.save(message);

        conversationInternalService.updateTime(savedMessage.getConversation());

        simpMessagingTemplate.convertAndSend("/topic/conversation" + savedMessage.getConversation().getId(), savedMessage);
        simpMessagingTemplate.convertAndSend("/topic/conversation-updates", savedMessage.getConversation().getId());
    }

    @Override
    public Page<Message> findAllByConversation(UUID conversationId, int page, int size) {
        return messageInternalService.findAllByConversation(conversationId, page, size);
    }

    @Override
    public void edit(UUID messageId, String content) {
        Message message = messageInternalService.findById(messageId);
        message.setContent(content);
        message.setEdited(true);
        Message savedMessage = messageRepository.save(message);

        conversationInternalService.updateTime(savedMessage.getConversation());

        simpMessagingTemplate.convertAndSend("/topic/conversation" + savedMessage.getConversation().getId(), savedMessage);
        simpMessagingTemplate.convertAndSend("/topic/conversation-updates", savedMessage.getConversation().getId());
    }

    @Override
    public void markAsRead(UUID conversationId, ReaderType readerType) {
        List<Message> unreadMessages;

        if (readerType == ReaderType.AGENT) {
            unreadMessages = messageInternalService.findUnreadMessagesBySenderByConversation(
                conversationId, SenderType.CUSTOMER);
        } else if (readerType == ReaderType.CUSTOMER) {
            unreadMessages = messageInternalService.findUnreadMessagesBySenderByConversation(
                conversationId, SenderType.AGENT);
        } else {
            throw new IllegalArgumentException("Invalid readerType");
        }

        for (Message message : unreadMessages) {
            message.setRead(true);
        }

        messageRepository.saveAll(unreadMessages);

        List<UUID> readMessageIds = unreadMessages.stream().map(Message::getId).collect(Collectors.toList());

        simpMessagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/read", readMessageIds);
        simpMessagingTemplate.convertAndSend("/topic/conversation-updates", conversationId);
    }
}
