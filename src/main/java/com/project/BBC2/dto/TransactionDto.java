package com.project.BBC2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    private String customerId;
    private long invoiceId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private Boolean isEarly;
    private String invoice_status;


    private long cardId;

    private double walletBalance;

}
