package com.project.BBC2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardDetailsDto {
        private String customerId;
        private String cardNumber;
        private String cardHolderName;
        private String expiryDate;
        private String cvv;
        private String cardType;
        private String cardId;
        private BigDecimal balance;
}