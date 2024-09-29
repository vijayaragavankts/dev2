package com.project.BBC2.service;

import com.project.BBC2.dto.TransactionDto;
import com.project.BBC2.exception.CustomerNotFoundException;
import com.project.BBC2.exception.InvalidTransactionException;
import com.project.BBC2.exception.InvoiceNotFoundException;
import com.project.BBC2.model.*;
import com.project.BBC2.model.BankInfo.CardDetails;
import com.project.BBC2.model.BankInfo.WalletDetails;
import com.project.BBC2.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private InvoiceRepo invoiceRepo;

    @Autowired
    private CardDetailsRepo cardDetailsRepo;

    @Autowired
    private WalletDetailsRepo walletDetailsRepo;

    public Transaction createTransaction(TransactionDto transactionDto) {
        validateTransactionDto(transactionDto);

        Customer customer = customerRepo.findByCustomerId(transactionDto.getCustomerId())
                .orElseThrow(()->new CustomerNotFoundException(transactionDto.getCustomerId()));
        if (customer == null) {
            throw new CustomerNotFoundException(transactionDto.getCustomerId());
        }

        Invoice invoice = invoiceRepo.findById(transactionDto.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(transactionDto.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidTransactionException("The invoice has already been fully paid. No further transactions are allowed.");
        }

        if (!invoice.getCustomer().getCustomerId().equals(transactionDto.getCustomerId())) {
            throw new InvalidTransactionException("The customer does not have permission to pay this invoice.");
        }

        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setInvoice(invoice);
        transaction.setIsEarly(transactionDto.getIsEarly());
        transaction.setPaymentMethod(transactionDto.getPaymentMethod());

        BigDecimal transactionAmount = transactionDto.getAmount();
        boolean isEarly = transactionDto.getIsEarly();

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (invoice.getStatus() != InvoiceStatus.PARTIALLY &&
                (transactionDto.getPaymentMethod().equalsIgnoreCase("CARD")
                        || (transactionDto.getPaymentMethod().equalsIgnoreCase("WALLET"))) ){


            // Apply discount logic
            if (isEarly) {
                // Apply double discount for early payments
                discountAmount = invoice.getDouble_discount_amount();
            } else {
                // Apply single discount for late payments
                discountAmount = invoice.getSingle_discount_amount();
            }

        } else if(invoice.getStatus() != InvoiceStatus.PARTIALLY && transactionDto.getPaymentMethod().equalsIgnoreCase("CASH")){
            if(isEarly){
                discountAmount = invoice.getSingle_discount_amount();
            }
            else{
                discountAmount = invoice.getAmount();
            }
            // For non-card payments, use the original transaction amount
        }
        else if(invoice.getStatus() == InvoiceStatus.PARTIALLY){
            discountAmount = invoice.getAmount();
        }


        // Process the transaction based on the payment amount
        if (transactionAmount.compareTo(discountAmount) < 0) {
            try{
                handlePartialPayment(invoice, transaction, transactionDto, transactionAmount);
            }
            catch (Exception e) {
                transaction.setStatus("FAILED");
                transaction.setInvoice_status("FAILED");
                logger.error("Partial payment transaction failed: {}", e.getMessage());
            }

        } else {
            try{
                handleFullPayment(invoice, transactionDto, transaction, transactionAmount, isEarly,discountAmount);
            }
            catch (Exception e) {
                transaction.setStatus("FAILED");
                transaction.setInvoice_status("FAILED");
                logger.error("transaction failed: {}", e.getMessage());
            }

        }

        return transactionRepo.save(transaction);
    }

    private void handlePartialPayment(Invoice invoice, Transaction transaction, TransactionDto transactionDto, BigDecimal transactionAmount) {
        try {
            if (transactionDto.getPaymentMethod().equalsIgnoreCase("CARD")) {
                validateCardDetails(transactionDto);
                CardDetails cardDetails = new CardDetails();
                cardDetails.setCustomer(transaction.getCustomer());
                cardDetails.setCardNumber(transactionDto.getCardNumber());
                cardDetails.setCardHolderName(transactionDto.getCardHolderName());
                cardDetails.setExpiryDate(transactionDto.getCardExpiryDate());
                cardDetails.setCvv(transactionDto.getCvv());
                cardDetails.setCardType(transactionDto.getCardType());
                cardDetailsRepo.save(cardDetails);

                transaction.setCardDetails(cardDetails);
            } else if (transactionDto.getPaymentMethod().equalsIgnoreCase("WALLET")) {
                WalletDetails walletDetails = walletDetailsRepo.findByCustomer(transaction.getCustomer())
                        .orElseThrow(() -> new InvalidTransactionException("Wallet not found for the customer."));

                BigDecimal walletBalance = walletDetails.getBalance();
                if (walletBalance.compareTo(transactionAmount) < 0) {
                    throw new InvalidTransactionException("Insufficient wallet balance.");
                }
                walletDetails.debit(transactionDto.getAmount()); // Deduct from wallet
                walletDetailsRepo.save(walletDetails);
                transaction.setWalletDetails(walletDetails);
            }
            transaction.setAmount(transactionAmount);
            transaction.setStatus("SUCCESS"); // Assume successful for partial payment
            transaction.setInvoice_status("PARTIAL");
            invoice.setAmount(invoice.getAmount().subtract(transactionAmount));
            invoice.setStatus(InvoiceStatus.PARTIALLY); // Update invoice status
            invoice.setDouble_discount_amount(BigDecimal.valueOf(0));
            invoice.setSingle_discount_amount(BigDecimal.valueOf(0));
            invoiceRepo.save(invoice);

        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
            logger.error("Partial payment transaction failed: {}", e.getMessage());
        }
    }


    private void handleFullPayment(Invoice invoice, TransactionDto transactionDto, Transaction transaction,
                                   BigDecimal transactionAmount, boolean isEarly,BigDecimal discountAmount) {
        validateFullPayment(invoice, transactionDto, transaction, transactionAmount, isEarly ,discountAmount);

        try {
            processPayment(transactionDto, transaction, invoice,discountAmount);
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
            logger.error("Transaction failed: {}", e.getMessage());
        }
    }

    private void validateFullPayment(Invoice invoice, TransactionDto transactionDto, Transaction transaction,
                                     BigDecimal transactionAmount, boolean isEarly, BigDecimal discountAmount) {

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidTransactionException("Invoice has already been fully paid.");
        }

        if(transactionAmount.compareTo(discountAmount) > 0){
            // User entered greater amount than the bill amount
            throw new InvalidTransactionException("Payment amount exceeds the invoice amount");
        }

//        if (isEarly && (transaction.getPaymentMethod().equalsIgnoreCase("CARD") ||
//                transactionDto.getPaymentMethod().equalsIgnoreCase("WALLET")) &&
//                transactionAmount.compareTo(invoice.getDouble_discount_amount()) != 0) {
//            throw new InvalidTransactionException("Payment amount must be equal to the invoice amount. Early payment required.");
//        }
        // Additional conditions as per your business rules
    }

    private void processPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice,BigDecimal discountAmount) {
        try {
            switch (transactionDto.getPaymentMethod().toUpperCase()) {
                case "CARD":
                    handleCardPayment(transactionDto, transaction, invoice, discountAmount);
                    break;
                case "WALLET":
                    handleWalletPayment(transactionDto, transaction, invoice, discountAmount);
                    break;
                case "CASH":
                    handleCashPayment(transactionDto, transaction, invoice);
                    break;
                default:
                    throw new InvalidTransactionException("Invalid payment method.");
            }
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
            logger.error("Payment processing failed: {}", e.getMessage());
            throw e;  // Rethrow to ensure consistent handling
        }
    }

    private void handleCardPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice,BigDecimal discountAmount) {
        validateCardDetails(transactionDto);

        CardDetails cardDetails = new CardDetails();
        cardDetails.setCustomer(transaction.getCustomer());
        cardDetails.setCardNumber(transactionDto.getCardNumber());
        cardDetails.setCardHolderName(transactionDto.getCardHolderName());
        cardDetails.setExpiryDate(transactionDto.getCardExpiryDate());
        cardDetails.setCvv(transactionDto.getCvv());
        cardDetails.setCardType(transactionDto.getCardType());
        cardDetailsRepo.save(cardDetails);

        transaction.setCardDetails(cardDetails);  // Associate the card details

        transaction.setAmount(discountAmount);


        // Check if the invoice has been partially paid


        boolean cardPaymentSuccess = simulateCardPayment(cardDetails, transaction.getAmount());
        if (cardPaymentSuccess) {
            transaction.setStatus("SUCCESS");
            invoice.setAmount(invoice.getAmount().subtract(discountAmount));
            invoice.setStatus(InvoiceStatus.PAID);
            transaction.setInvoice_status("FULL");
        } else {
            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
        }
    }

    private void handleWalletPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice,BigDecimal discountAmount) {
        WalletDetails walletDetails = walletDetailsRepo.findByCustomer(transaction.getCustomer())
                .orElseThrow(() -> new InvalidTransactionException("Wallet not found for the customer."));

        BigDecimal walletBalance = walletDetails.getBalance();
        if (walletBalance.compareTo(transactionDto.getAmount()) < 0) {
            throw new InvalidTransactionException("Insufficient wallet balance.");
        }

        walletDetails.debit(transactionDto.getAmount()); // Deduct from wallet
        walletDetailsRepo.save(walletDetails);
        transaction.setWalletDetails(walletDetails);  // Associate the wallet details
        transaction.setStatus("SUCCESS");
        transaction.setAmount(discountAmount);
        invoice.setAmount(invoice.getAmount().subtract(discountAmount));
        invoice.setStatus(InvoiceStatus.PAID);
        transaction.setInvoice_status("FULL");
    }

    private void handleCashPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice) {
        transaction.setStatus("SUCCESS");
        transaction.setAmount(transactionDto.getAmount());
        invoice.setStatus(InvoiceStatus.PAID);
        transaction.setInvoice_status("FULL");
    }

    private boolean simulateCardPayment(CardDetails cardDetails, BigDecimal amount) {
        return true; // Simulate successful payment
    }

    private void validateCardDetails(TransactionDto transactionDto) {
        if (transactionDto.getCardNumber() == null || transactionDto.getCardNumber().length() != 16) {
            throw new InvalidTransactionException("Card number must be 16 digits.");
        }
        if (transactionDto.getCvv() == null || transactionDto.getCvv().length() != 3) {
            throw new InvalidTransactionException("CVV must be 3 digits.");
        }
        if (!isExpiryDateValid(transactionDto.getCardExpiryDate())) {
            throw new InvalidTransactionException("Card expiry date must be in the future.");
        }
    }

    private boolean isExpiryDateValid(String expiryDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy");
            sdf.setLenient(false);
            Date date = sdf.parse(expiryDate);
            return date.after(new Date());
        } catch (ParseException e) {
            throw new InvalidTransactionException("Invalid expiry date format. Use MM/yyyy.");
        }
    }

    public Transaction getTransactionById(long id) {
        return transactionRepo.findById(id).orElse(null);
    }

    public List<Transaction> getTransactionsByCustomerId(String customerId) {
        return transactionRepo.findByCustomer_CustomerId(customerId);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepo.findAll();
    }

    private void validateTransactionDto(TransactionDto transactionDto) {
        if (transactionDto.getInvoiceId() <= 0) {
            throw new InvalidTransactionException("Invalid invoice ID: " + transactionDto.getInvoiceId());
        }
        if (transactionDto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Transaction amount cannot be negative: " + transactionDto.getAmount());
        }
        if (transactionDto.getPaymentMethod() == null || transactionDto.getPaymentMethod().isEmpty()) {
            throw new InvalidTransactionException("Payment method cannot be null or empty.");
        }
        if (transactionDto.getPaymentMethod().equalsIgnoreCase("CARD")) {
            validateCardDetails(transactionDto);
        }
    }
}