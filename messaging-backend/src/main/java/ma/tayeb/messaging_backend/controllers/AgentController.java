package ma.tayeb.messaging_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.agent.AgentCreationRequest;
import ma.tayeb.messaging_backend.entities.Agent;
import ma.tayeb.messaging_backend.services.interfaces.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentService agentService;

    @PostMapping("/find-or-create")
    public ResponseEntity<Agent> findOrCreate(@RequestBody AgentCreationRequest request) {
        return new ResponseEntity<>(agentService.findOrCreate(request), HttpStatus.OK);
    }
}
    
