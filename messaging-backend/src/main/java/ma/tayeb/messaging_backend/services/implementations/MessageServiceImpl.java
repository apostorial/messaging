package ma.tayeb.messaging_backend.services.implementations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.MessageCreationRequest;
import ma.tayeb.messaging_backend.dtos.MessageEditRequest;
import ma.tayeb.messaging_backend.dtos.MessageReadRequest;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.MessageType;
import ma.tayeb.messaging_backend.enums.ReaderType;
import ma.tayeb.messaging_backend.enums.SenderType;
import ma.tayeb.messaging_backend.repositories.AgentRepository;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;
import ma.tayeb.messaging_backend.repositories.CustomerRepository;
import ma.tayeb.messaging_backend.repositories.MessageRepository;
import ma.tayeb.messaging_backend.services.interfaces.FileService;
import ma.tayeb.messaging_backend.services.interfaces.MessageService;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final AgentRepository agentRepository;
    private final CustomerRepository customerRepository;
    private final FileService fileService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void send(MessageCreationRequest request) {
        SenderType senderType = determineSenderType(request.getSenderId());

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: `" + request.getConversationId() + "`"));

        Message replyTo;
        if (StringUtils.hasText(request.getReplyToId().toString())) {
            replyTo = messageRepository.findById(request.getReplyToId())
                .orElseThrow(() -> new RuntimeException("Message not found with ID: `" + request.getReplyToId() + "`"));   
        } else {
            replyTo = null;
        }

        Message message = Message.builder()
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .senderId(request.getSenderId())
                .senderType(senderType)
                .messageType(MessageType.TEXT)
                .replyTo(replyTo)
                .conversation(conversation)
                .build();
        
        Message savedMessage = messageRepository.save(message);

        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        simpMessagingTemplate.convertAndSend("/topic/conversation" + savedMessage.getConversation().getId(), savedMessage);
        simpMessagingTemplate.convertAndSend("/topic/conversations", savedMessage.getConversation().getId());
    }

    @Override
    public void uploadAndSend(MultipartFile file, MessageCreationRequest request) {
        SenderType senderType = determineSenderType(request.getSenderId());

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: `" + request.getConversationId() + "`"));

        Message replyTo;
        if (StringUtils.hasText(request.getReplyToId().toString())) {
            replyTo = messageRepository.findById(request.getReplyToId())
                .orElseThrow(() -> new RuntimeException("Message not found with ID: `" + request.getReplyToId() + "`"));   
        } else {
            replyTo = null;
        }

        String fileUrl = fileService.upload(file);

        MessageType messageType;

        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image")) {
            messageType = MessageType.IMAGE;
        } else {
            messageType = MessageType.DOCUMENT;
        }

        Message message = Message.builder()
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .senderId(request.getSenderId())
                .senderType(senderType)
                .messageType(messageType)
                .fileUrl(fileUrl)
                .replyTo(replyTo)
                .conversation(conversation)
                .build();
        
        Message savedMessage = messageRepository.save(message);

        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        simpMessagingTemplate.convertAndSend("/topic/conversation" + savedMessage.getConversation().getId(), savedMessage);
        simpMessagingTemplate.convertAndSend("/topic/conversations", savedMessage.getConversation().getId());
    }

    @Override
    public Page<Message> findAllByConversation(UUID conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId, pageable);
    }

    @Override
    public void edit(MessageEditRequest request) {
        Message message = messageRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Message not found with ID: `" + request.getId() + "`"));
        message.setContent(request.getContent());
        message.setEdited(true);
        Message savedMessage = messageRepository.save(message);
        
        Conversation conversation = message.getConversation();
        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        simpMessagingTemplate.convertAndSend("/topic/conversation" + savedMessage.getConversation().getId(), savedMessage);
        simpMessagingTemplate.convertAndSend("/topic/conversations", savedMessage.getConversation().getId());
    }

    @Override
    public void markAsRead(MessageReadRequest request) {
        List<Message> unreadMessages;

        if (request.getReaderType() == ReaderType.AGENT) {
            unreadMessages = messageRepository.findByConversationIdAndSenderTypeAndReadFalse(
                request.getConversationId(), SenderType.CUSTOMER);
        } else if (request.getReaderType() == ReaderType.CUSTOMER) {
            unreadMessages = messageRepository.findByConversationIdAndSenderTypeAndReadFalse(
                request.getConversationId(), SenderType.AGENT);
        } else {
            throw new IllegalArgumentException("Invalid readerType");
        }

        for (Message message : unreadMessages) {
            message.setRead(true);
        }

        messageRepository.saveAll(unreadMessages);

        List<UUID> readMessageIds = unreadMessages.stream().map(Message::getId).collect(Collectors.toList());
        simpMessagingTemplate.convertAndSend(
            "/topic/conversation/" + request.getConversationId() + "/read-receipt",
            readMessageIds
        );
        simpMessagingTemplate.convertAndSend("/topic/conversations", request.getConversationId());
        
    }

    private SenderType determineSenderType(UUID senderId) {
        if (agentRepository.existsById(senderId)) {
            return SenderType.AGENT;
        } else if (customerRepository.existsById(senderId)) {
            return SenderType.CUSTOMER;
        } else {
            throw new RuntimeException("Sender not found with ID: `" + senderId + "`");
        }
    }
}
