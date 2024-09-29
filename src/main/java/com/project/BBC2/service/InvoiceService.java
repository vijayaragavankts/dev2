package com.project.BBC2.service;

import com.project.BBC2.model.Customer;
import com.project.BBC2.model.Invoice;
import com.project.BBC2.repository.CustomerRepo;
import com.project.BBC2.repository.InvoiceRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepo invoiceRepo;

    @Autowired
    private CustomerRepo customerRepo;

    public List<Invoice> getInvoicesByCustomerId(String customerId) {
        // First, find the customer by ID
        Customer customer = customerRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Now find invoices by customer
        return invoiceRepo.findByCustomer(customer);
    }
}
