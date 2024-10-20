package com.project.BBC2.controller;

import com.project.BBC2.dto.TransactionDto;
import com.project.BBC2.model.Transaction;
import com.project.BBC2.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:4200")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionDto transactionDto) {
        return transactionService.createTransaction(transactionDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return transaction != null ? new ResponseEntity<>(transaction, HttpStatus.OK) :
                new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getTransactionsByCustomerId(@PathVariable String customerId) {
        List<Transaction> transactions = transactionService.getTransactionsByCustomerId(customerId);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }
}
