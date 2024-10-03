package com.project.BBC2.service;

import com.project.BBC2.dto.CardDetailsDto;
import com.project.BBC2.model.BankInfo.CardDetails;
import com.project.BBC2.model.Customer;
import com.project.BBC2.repository.CardDetailsRepo;
import com.project.BBC2.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CardService {

    @Autowired
    private CardDetailsRepo cardDetailsRepo;

    @Autowired
    private CustomerRepo customerRepo;

    public List<CardDetails> getCardDetailsByCustomerId(String customerId) {
        return cardDetailsRepo.findByCustomer_CustomerId(customerId);
    }

    // New method to get card details by cardId
    public CardDetails getCardDetailsByCardId(Long cardId) {
        Optional<CardDetails> cardDetails = cardDetailsRepo.findById(cardId);
        return cardDetails.orElse(null); // Or throw an exception if card not found
    }

    public CardDetails saveCardDetails(CardDetailsDto cardDetailsDto) {
        CardDetails cardDetails = new CardDetails();
        cardDetails.setCardNumber(cardDetailsDto.getCardNumber());
        cardDetails.setCardHolderName(cardDetailsDto.getCardHolderName());
        cardDetails.setExpiryDate(cardDetailsDto.getExpiryDate());
        cardDetails.setCvv(cardDetailsDto.getCvv());
        cardDetails.setCardType(cardDetailsDto.getCardType());
        cardDetails.setBalance(BigDecimal.valueOf(5000));
        // Fetch the customer and set it
        Customer customer = customerRepo.findByCustomerId(cardDetailsDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        cardDetails.setCustomer(customer);

        return cardDetailsRepo.save(cardDetails);
    }

    public CardDetails creditCard(Long cardId, BigDecimal amount) {
        // Find the card by ID
        CardDetails cardDetails = cardDetailsRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Credit the specified amount to the card
        cardDetails.credit(amount);

        // Save the updated card details and return
        return cardDetailsRepo.save(cardDetails);
    }

    public CardDetails findById(Long cardId) {
        return cardDetailsRepo.findById(cardId).orElse(null);
    }

    public CardDetails save(CardDetails cardDetails) {
        return cardDetailsRepo.save(cardDetails);
    }



}
