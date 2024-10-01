package com.project.BBC2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardDetailsDto {
        private String customerId;       // ID of the customer to whom the card belongs
        private String cardNumber;        // Card number
        private String cardHolderName;    // Name of the cardholder
        private String expiryDate;    // Expiry date of the card (e.g., MM/YY)
        private String cvv;               // Card Verification Value
        private String cardType;
        private String cardId;
}