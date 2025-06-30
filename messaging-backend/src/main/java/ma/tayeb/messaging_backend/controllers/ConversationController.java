package ma.tayeb.messaging_backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.customer.CustomerIdentifierRequest;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.services.interfaces.ConversationService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController @RequiredArgsConstructor @RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;
    
    @GetMapping("/customer")
    public ResponseEntity<Conversation> findByCustomer(@RequestBody CustomerIdentifierRequest request) {
        return new ResponseEntity<>(conversationService.findByCustomer(request), HttpStatus.OK);
    }
    
    @GetMapping
    public ResponseEntity<Page<Conversation>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(conversationService.findAll(page, size), HttpStatus.OK);
    }
}
