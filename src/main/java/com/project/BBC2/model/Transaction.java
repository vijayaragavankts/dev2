package com.project.BBC2.model;

import com.project.BBC2.model.BankInfo.CardDetails;
import com.project.BBC2.model.BankInfo.WalletDetails;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long transactionId;  // Unique ID for the transaction

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;    // The customer associated with the transaction

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;      // The invoice related to the transaction

    private BigDecimal amount;         // The amount of the transaction
    private String status;         // Status of the transaction (e.g., SUCCESS, FAILED)
    private String invoice_status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime transactionDate; // Date when the transaction was made

    private String paymentMethod; // Payment method (e.g., "CASH", "ONLINE", etc.)

    private Boolean isEarly;


    private Long methodId;
}
