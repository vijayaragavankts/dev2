package com.project.BBC2.repository;

import com.project.BBC2.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction,Long> {
    List<Transaction> findByCustomer_CustomerId(String customerId);


    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :transactionId AND t.status = :status")
    Transaction findByInvoiceIdAndStatus(@Param("transactionId") Long invoiceId, @Param("status") String status);

}
