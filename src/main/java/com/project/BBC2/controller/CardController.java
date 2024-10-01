package com.project.BBC2.controller;

import com.project.BBC2.dto.CardDetailsDto;
import com.project.BBC2.model.BankInfo.CardDetails;
import com.project.BBC2.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/card-details")
@CrossOrigin(origins = "http://localhost:4200")
public class CardController {
    @Autowired
    private CardService cardService;

    @GetMapping("/{customerId}")
    public ResponseEntity<List<CardDetails>> getCardDetails(@PathVariable String customerId) {
        List<CardDetails> cardDetailsList = cardService.getCardDetailsByCustomerId(customerId);
        return ResponseEntity.ok(cardDetailsList);
    }

    // New method to get card details by cardId
    @GetMapping("/card/{cardId}")
    public ResponseEntity<CardDetails> getCardDetailsByCardId(@PathVariable Long cardId) {
        CardDetails cardDetails = cardService.getCardDetailsByCardId(cardId);
        return ResponseEntity.ok(cardDetails);
    }

    @PostMapping
    public ResponseEntity<CardDetails> createCardDetails(@RequestBody CardDetailsDto cardDetailsDto) {
        CardDetails savedCardDetails = cardService.saveCardDetails(cardDetailsDto);
        return ResponseEntity.ok(savedCardDetails);
    }
}
