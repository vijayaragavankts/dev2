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

    private long transactionId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    private BigDecimal amount;
    private String status;
    private String invoice_status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime transactionDate;

    private String paymentMethod;

    private Boolean isEarly;


    private Long methodId;
}
