package com.project.BBC2.repository;

import com.project.BBC2.model.BankInfo.CardDetails;
import com.project.BBC2.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardDetailsRepo extends JpaRepository<CardDetails,Long> {
    Optional<CardDetails> findByCustomerAndCardNumber(Customer customer, String cardNumber);
}
