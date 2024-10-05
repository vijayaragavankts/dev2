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
    public ResponseEntity<?> validateCustomerAndSendOtp(@RequestParam String customerId) {
        try {
            return customerService.validateCustomerAndSendOtp(customerId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/validateOtp")
    public ResponseEntity<?> validateOtp(@RequestParam String enteredOtp) {
        if (enteredOtp == null || enteredOtp.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "OTP cannot be empty"));
        }
        return customerService.validateOtp(enteredOtp);
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
