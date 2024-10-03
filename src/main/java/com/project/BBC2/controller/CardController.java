package com.project.BBC2.controller;

import com.project.BBC2.dto.CardDetailsDto;
import com.project.BBC2.model.BankInfo.CardDetails;
import com.project.BBC2.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @PostMapping("/{cardId}/credit")
    public ResponseEntity<CardDetails> creditCard(@PathVariable Long cardId, @RequestParam BigDecimal amount) {
        CardDetails updatedCardDetails = cardService.creditCard(cardId, amount);
        return ResponseEntity.ok(updatedCardDetails);
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<CardDetails> updateCard(@PathVariable Long cardId, @RequestBody CardDetails updatedCardDetails) {
        CardDetails existingCard = cardService.findById(cardId);
        if (existingCard == null) {
            return ResponseEntity.notFound().build();
        }

        // Update fields
        existingCard.setCardNumber(updatedCardDetails.getCardNumber());
        existingCard.setCardHolderName(updatedCardDetails.getCardHolderName());
        existingCard.setExpiryDate(updatedCardDetails.getExpiryDate());
        existingCard.setCvv(updatedCardDetails.getCvv());
        existingCard.setCardType(updatedCardDetails.getCardType());
        existingCard.setBalance(updatedCardDetails.getBalance()); // Update balance if needed

        CardDetails updatedCard = cardService.save(existingCard);
        return ResponseEntity.ok(updatedCard);
    }

}
