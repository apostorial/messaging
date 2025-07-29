package ma.tayeb.messaging_backend.controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.conversation.ConversationResponse;
import ma.tayeb.messaging_backend.services.interfaces.conversation.ConversationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<Page<ConversationResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(conversationService.findAll(page, size), HttpStatus.OK);
    }
}
