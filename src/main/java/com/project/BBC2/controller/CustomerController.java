package com.project.BBC2.controller;

import com.project.BBC2.model.Customer;
import com.project.BBC2.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:4200")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/validate")
    public ResponseEntity<String> validateCustomerAndSendOtp(@RequestParam String customerId) {
        try {
            String response = customerService.validateCustomerAndSendOtp(customerId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/validateOtp")
    public ResponseEntity<String> validateOtp(@RequestParam String enteredOtp) {
        boolean isValid = customerService.validateOtp(enteredOtp);
        if (isValid) {
            return ResponseEntity.ok("OTP is valid.");
        } else {
            return ResponseEntity.ok("Invalid OTP.");
        }
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String customerId) {
        try {
            Customer customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
