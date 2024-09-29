package com.project.BBC2.service;

import com.project.BBC2.model.Customer;
import com.project.BBC2.model.BankInfo.WalletDetails;
import com.project.BBC2.repository.CustomerRepo;
import com.project.BBC2.repository.WalletDetailsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    @Autowired
    private WalletDetailsRepo walletDetailsRepo;

    @Autowired
    private CustomerRepo customerRepo;


    // Method to credit a wallet
    public void creditWallet(String customerId, BigDecimal amount) {
        Customer customer = customerRepo.findByCustomerId(customerId)
                .orElseThrow(()->new IllegalArgumentException("Customer not found"));
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }

        WalletDetails walletDetails = walletDetailsRepo.findByCustomer(customer)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for the customer"));

        walletDetails.credit(amount);  // Add the amount to the wallet balance
        walletDetailsRepo.save(walletDetails);  // Save the updated wallet balance
    }

    // Method to debit a wallet
    public void debitWallet(String customerId, BigDecimal amount) {
        Customer customer = customerRepo.findByCustomerId(customerId)
                .orElseThrow(()->new IllegalArgumentException("Customer not found"));
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }

        WalletDetails walletDetails = walletDetailsRepo.findByCustomer(customer)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for the customer"));

        walletDetails.debit(amount);  // Deduct the amount from the wallet balance
        walletDetailsRepo.save(walletDetails);  // Save the updated wallet balance
    }
}
