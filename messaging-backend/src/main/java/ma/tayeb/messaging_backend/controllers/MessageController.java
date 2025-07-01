package ma.tayeb.messaging_backend.controllers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.MessageRequest;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.MessageType;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.repositories.MessageRepository;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;
import ma.tayeb.messaging_backend.services.FileService;
import ma.tayeb.messaging_backend.enums.SenderType;
import ma.tayeb.messaging_backend.repositories.AgentRepository;
import ma.tayeb.messaging_backend.repositories.CustomerRepository;
import ma.tayeb.messaging_backend.dtos.ConversationSummary;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final AgentRepository agentRepository;
    private final CustomerRepository customerRepository;
    private final FileService fileService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/conversations/{conversationId}/messages")
    public List<Message> getConversationMessages(@PathVariable UUID conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(MessageRequest chatMessage) {
        SenderType senderType = determineSenderType(chatMessage.getSenderId());

        Conversation conversation = conversationRepository.findById(chatMessage.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setSenderId(chatMessage.getSenderId());
        message.setSenderType(senderType);
        message.setContent(chatMessage.getContent());
        message.setTimestamp(LocalDateTime.now());
        message.setMessageType(MessageType.TEXT);
        message.setReplyTo(chatMessage.getReplyTo());
        message.setRead(false);
        Message savedMessage = messageRepository.save(message);

        // Update conversation's lastUpdated timestamp
        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        messagingTemplate.convertAndSend("/topic/conversation/" + savedMessage.getConversation().getId(), savedMessage);
    }

    @PostMapping("/upload")
    public void uploadFile(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute MessageRequest messageRequest) {

        SenderType senderType = determineSenderType(messageRequest.getSenderId());
        Conversation conversation = conversationRepository.findById(messageRequest.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        String fileUrl = fileService.upload(file);
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setSenderId(messageRequest.getSenderId());
        message.setSenderType(senderType);
        message.setFileUrl(fileUrl);
        message.setTimestamp(LocalDateTime.now());
        message.setReplyTo(messageRequest.getReplyTo());
        message.setContent(messageRequest.getContent());
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image")) {
            message.setMessageType(MessageType.IMAGE);
        } else {
            message.setMessageType(MessageType.DOCUMENT);
        }
        Message savedMessage = messageRepository.save(message);

        // Update conversation's lastUpdated timestamp
        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        messagingTemplate.convertAndSend("/topic/conversation/" + savedMessage.getConversation().getId(), savedMessage);
    }

    @PutMapping("/messages/{id}")
    public Message editMessage(@PathVariable UUID id, @RequestBody MessageRequest messageRequest) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setContent(messageRequest.getContent());
        message.setEdited(true);
        Message savedMessage = messageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + savedMessage.getConversation().getId(), savedMessage);
        return savedMessage;
    }

    @PutMapping("/messages/{id}/edit")
    public Message editMessageContent(@PathVariable UUID id, @RequestParam("content") String content) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setContent(content);
        message.setEdited(true);
        Message savedMessage = messageRepository.save(message);

        // Update conversation's lastUpdated timestamp
        Conversation conversation = savedMessage.getConversation();
        conversation.setLastUpdated(LocalDateTime.now());
        conversationRepository.save(conversation);

        messagingTemplate.convertAndSend("/topic/conversation/" + savedMessage.getConversation().getId(), savedMessage);
        return savedMessage;
    }

    @GetMapping("/conversations")
    public List<ConversationSummary> getAllConversations() {
        return conversationRepository.findAllByOrderByLastUpdatedDesc().stream()
                .map(conv -> {
                    Message lastMessage = messageRepository.findTopByConversationOrderByTimestampDesc(conv);
                    return new ConversationSummary(
                            conv.getId(),
                            conv.getOwner().getFullName(),
                            lastMessage != null ? lastMessage.getTimestamp() : null,
                            lastMessage != null ? lastMessage.getContent() : null);
                })
                .collect(Collectors.toList());
    }

    @PutMapping("/conversations/{conversationId}/read")
    public void markMessagesAsRead(@PathVariable UUID conversationId) {
        List<Message> unreadMessages = messageRepository.findByConversationIdAndReadFalse(conversationId);
        for (Message msg : unreadMessages) {
            msg.setRead(true);
        }
        messageRepository.saveAll(unreadMessages);
    }

    private SenderType determineSenderType(UUID senderId) {
        if (agentRepository.existsById(senderId)) {
            return SenderType.AGENT;
        } else if (customerRepository.existsById(senderId)) {
            return SenderType.CUSTOMER;
        } else {
            throw new RuntimeException("Sender not found with ID: " + senderId);
        }
    }
}