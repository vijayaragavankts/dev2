package com.project.BBC2.service;

import com.project.BBC2.model.Customer;
import com.project.BBC2.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
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

    public String validateCustomerAndSendOtp(String customerId) {
        // Check if customer exists
        Optional<Customer> customerOpt = customerRepo.findByCustomerId(customerId);

        if (customerOpt.isPresent()) {
            // Generate 6-digit OTP
            String otp = generateOtp();

            // Get customer email
            String email = customerOpt.get().getEmail();
            this.otp1 = otp;
            System.out.println("Otp : " + this.otp1);

            // Send OTP to email
            emailService.sendOtpEmail(email, otp);

            return "OTP sent to " + email;
        } else {
            throw new RuntimeException("Invalid Customer ID");
        }
    }
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates a number between 100000 and 999999
        return String.valueOf(otp);
    }

    public boolean validateOtp(String enteredOtp) {
        // Compare the stored OTP (otp1) with the entered OTP
        if (Objects.equals(this.otp1, enteredOtp)) {
            return true; // OTP is valid
        } else {
            return false; // OTP is invalid
        }
    }

    public Customer getCustomerById(String customerId) {
        return customerRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

}
