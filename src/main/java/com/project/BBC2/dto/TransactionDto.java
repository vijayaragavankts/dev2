package com.project.BBC2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    private String customerId;      // Customer ID associated with the transaction
    private long invoiceId;       // Invoice ID related to the transaction
    private BigDecimal amount;         // Amount of the transaction
    private String status;         // Status of the transaction
    private String paymentMethod;  // Payment method
    private Boolean isEarly;
    private String invoice_status;

    // Card Details (only used if paymentMethod is "CARD")
    private long cardId;

    // Wallet Details (only used if paymentMethod is "WALLET")
    private double walletBalance;   // Optional field to track wallet balance if needed

}
