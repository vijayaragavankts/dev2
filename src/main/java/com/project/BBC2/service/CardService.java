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

        Customer customer = customerRepo.findByCustomerId(cardDetailsDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        cardDetails.setCustomer(customer);

        return cardDetailsRepo.save(cardDetails);
    }

    public CardDetails creditCard(Long cardId, BigDecimal amount) {

        CardDetails cardDetails = cardDetailsRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));


        cardDetails.credit(amount);


        return cardDetailsRepo.save(cardDetails);
    }

    public CardDetails findById(Long cardId) {
        return cardDetailsRepo.findById(cardId).orElse(null);
    }

    public CardDetails save(CardDetails cardDetails) {
        return cardDetailsRepo.save(cardDetails);
    }



}
