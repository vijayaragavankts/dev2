package com.project.BBC2.model.BankInfo;

import com.project.BBC2.model.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.valueOf(0.0);

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;


    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }


    public void debit(BigDecimal amount) throws IllegalArgumentException {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }
}