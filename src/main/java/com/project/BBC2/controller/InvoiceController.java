package com.project.BBC2.controller;

import com.project.BBC2.model.Invoice;
import com.project.BBC2.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:4200")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoiceByCustomerId(@PathVariable String customerId){
        List<Invoice> invoices = invoiceService.getInvoicesByCustomerId(customerId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<Invoice> getInvoiceByInvoiceId(@PathVariable Long invoiceId) {
        Optional<Invoice> invoice = invoiceService.getInvoiceByInvoiceId(invoiceId);
        if (invoice.isPresent()) {
            return ResponseEntity.ok(invoice.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}