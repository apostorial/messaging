package ma.tayeb.messaging_backend.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.MessageCreationRequest;
import ma.tayeb.messaging_backend.dtos.MessageEditRequest;
import ma.tayeb.messaging_backend.dtos.MessageReadRequest;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.ReaderType;
import ma.tayeb.messaging_backend.services.interfaces.MessageService;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    @MessageMapping("/chat.sendMessage")
    public ResponseEntity<Void> send(MessageCreationRequest request) {
        messageService.send(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/upload-and-send")
    public ResponseEntity<Void> uploadAndSend(
            @RequestParam MultipartFile file,
            @ModelAttribute MessageCreationRequest request) {
        messageService.uploadAndSend(file, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<Message>> findAllByConversation(
            @PathVariable UUID conversationId,
            @RequestParam ReaderType readerType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(messageService.findAllByConversation(conversationId, page, size), HttpStatus.OK);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<Void> edit(
            @PathVariable UUID messageId,
            @RequestBody MessageEditRequest request) {
        messageService.edit(messageId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestBody MessageReadRequest request) {
        messageService.markAsRead(conversationId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
