package com.project.BBC2.service;

import com.project.BBC2.model.Customer;
import com.project.BBC2.model.Invoice;
import com.project.BBC2.repository.CustomerRepo;
import com.project.BBC2.repository.InvoiceRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepo invoiceRepo;

    @Autowired
    private CustomerRepo customerRepo;

    public List<Invoice> getInvoicesByCustomerId(String customerId) {

        Customer customer = customerRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));


        return invoiceRepo.findByCustomer(customer);
    }

    public Optional<Invoice> getInvoiceByInvoiceId(Long invoiceId) {
        return invoiceRepo.findById(invoiceId);
    }
}
