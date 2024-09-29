package com.project.BBC2.repository;

import com.project.BBC2.model.Customer;
import com.project.BBC2.model.BankInfo.WalletDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletDetailsRepo extends JpaRepository<WalletDetails,Long> {
    Optional<WalletDetails> findByCustomer(Customer customer);
}
