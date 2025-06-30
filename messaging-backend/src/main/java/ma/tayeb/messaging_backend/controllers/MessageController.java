package ma.tayeb.messaging_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.message.MessageCreationRequest;
import ma.tayeb.messaging_backend.dtos.message.MessageUpdateRequest;
import ma.tayeb.messaging_backend.entities.Message;
import ma.tayeb.messaging_backend.services.interfaces.MessageService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController @RequiredArgsConstructor @RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Message> create(@RequestBody MessageCreationRequest request) {
        return new ResponseEntity<>(messageService.create(request), HttpStatus.OK);
    }

    @PostMapping("/reply/{messageId}")
    public ResponseEntity<Message> reply(@PathVariable String messageId, @RequestBody MessageCreationRequest request) {
        return new ResponseEntity<>(messageService.reply(messageId, request), HttpStatus.OK);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<Message>> findAllByConversation(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(messageService.findAllByConversation(conversationId, page, size), HttpStatus.OK);
    }
    
    @PatchMapping("/{messageId}")
    public ResponseEntity<Message> update(@PathVariable String messageId, @RequestBody MessageUpdateRequest request) {
        return new ResponseEntity<>(messageService.update(messageId, request), HttpStatus.OK);
    }
}
