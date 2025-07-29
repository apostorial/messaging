package ma.tayeb.messaging_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.dtos.customer.CustomerCreationRequest;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.services.interfaces.CustomerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping("/find-or-create")
    public ResponseEntity<Customer> findOrCreate(@RequestBody CustomerCreationRequest request) {
        return new ResponseEntity<>(customerService.findOrCreate(request), HttpStatus.OK);
    }
}
    
