package ma.tayeb.messaging_backend.controllers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
        message.setType(MessageType.TEXT);

        Message savedMessage = messageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + savedMessage.getConversation().getId(), savedMessage);
    }

    @PostMapping("/upload")
    public void uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderId") UUID senderId,
            @RequestParam("conversationId") UUID conversationId) {

        SenderType senderType = determineSenderType(senderId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        String fileUrl = fileService.upload(file);

        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setFileUrl(fileUrl);
        message.setTimestamp(LocalDateTime.now());
        
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image")) {
            message.setType(MessageType.IMAGE);
            message.setContent(file.getOriginalFilename());
        } else {
            message.setType(MessageType.DOCUMENT);
            message.setContent(file.getOriginalFilename());
        }

        Message savedMessage = messageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + savedMessage.getConversation().getId(), savedMessage);
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