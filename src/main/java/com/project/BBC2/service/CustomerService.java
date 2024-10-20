package com.project.BBC2.service;

import com.project.BBC2.controller.ApiResponse;
import com.project.BBC2.model.Customer;
import com.project.BBC2.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private EmailService emailService;

    private String otp1 = "";

    public ResponseEntity<?> validateCustomerAndSendOtp(String customerId) {

        Optional<Customer> customerOpt = customerRepo.findByCustomerId(customerId);

        if (customerOpt.isPresent()) {

            String otp = generateOtp();


            String email = customerOpt.get().getEmail();
            this.otp1 = otp;
            System.out.println("Otp : " + this.otp1);

            // Send OTP to email
            emailService.sendOtpEmail(email, otp);

            return ResponseEntity.ok(new ApiResponse(true, "OTP sent to " + email));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Invalid Customer ID"));
        }
    }
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public ResponseEntity<?> validateOtp(String enteredOtp) {

        if (otp1.equals(enteredOtp)) {
            return ResponseEntity.ok(new ApiResponse(true, "OTP is valid"));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Invalid OTP"));
        }
    }

    public Customer getCustomerById(String customerId) {
        return customerRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
}