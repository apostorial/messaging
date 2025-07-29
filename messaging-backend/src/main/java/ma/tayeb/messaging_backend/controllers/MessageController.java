package ma.tayeb.messaging_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.enums.ReaderType;
import ma.tayeb.messaging_backend.services.interfaces.message.MessageService;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<Void> send(
            @ModelAttribute MessageCreationRequest request,
            @RequestParam(required = false) MultipartFile file) {
        messageService.send(request, file);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<Message>> findAllByConversation(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(messageService.findAllByConversation(conversationId, page, size), HttpStatus.OK);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<Void> edit(
            @PathVariable UUID messageId,
            @RequestParam String content) {
        messageService.edit(messageId, content);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestParam ReaderType readerType) {
        messageService.markAsRead(conversationId, readerType);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
