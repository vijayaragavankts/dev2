package com.project.BBC2.repository;

import com.project.BBC2.model.Customer;
import com.project.BBC2.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepo extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomer(Customer customer);
}
