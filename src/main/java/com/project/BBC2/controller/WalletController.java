package com.project.BBC2.controller;

import com.project.BBC2.model.BankInfo.WalletDetails;
import com.project.BBC2.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "http://localhost:4200")
public class WalletController {
    @Autowired
    private WalletService walletService;

    // Endpoint to credit the wallet
    @PostMapping("/credit/{customerId}/{amount}")
    public ResponseEntity<String> creditWallet(@PathVariable String customerId, @PathVariable BigDecimal amount) {
        try {
            walletService.creditWallet(customerId, amount);
            return ResponseEntity.ok("Successfully credited $" + amount + " to wallet of customer ID: " + customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while crediting the wallet.");
        }
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletDetails> getWalletDetails(@PathVariable Long walletId) {
        try {
            WalletDetails walletDetails = walletService.getWalletDetails(walletId);
            if (walletDetails != null) {
                return ResponseEntity.ok(walletDetails);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}