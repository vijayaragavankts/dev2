package com.project.BBC2.service;

import com.project.BBC2.controller.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    String curr = "";

    public ResponseEntity<?> createTransaction(TransactionDto transactionDto) {
        try {
            validateTransactionDto(transactionDto);

            Customer customer = customerRepo.findByCustomerId(transactionDto.getCustomerId()).orElse(null);

            if (customer == null) {
                return ResponseEntity.ok(new ApiResponse(false, "Customer not found with ID: " + transactionDto.getCustomerId()));
            }

            Invoice invoice = invoiceRepo.findById(transactionDto.getInvoiceId())
                    .orElseThrow(() -> new InvoiceNotFoundException(transactionDto.getInvoiceId()));

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return ResponseEntity.ok(new ApiResponse(false, "The invoice has already been fully paid. No further transactions are allowed."));
            }

            if (!invoice.getCustomer().getCustomerId().equals(transactionDto.getCustomerId())) {
                return ResponseEntity.ok(new ApiResponse(false,"The customer does not have permission to pay this invoice."));
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
                            || (transactionDto.getPaymentMethod().equalsIgnoreCase("WALLET")))) {


                // Apply discount logic
                if (isEarly) {
                    // Apply double discount for early payments
                    discountAmount = invoice.getDouble_discount_amount();
                } else {
                    // Apply single discount for late payments
                    discountAmount = invoice.getSingle_discount_amount();
                }

            } else if (invoice.getStatus() != InvoiceStatus.PARTIALLY && transactionDto.getPaymentMethod().equalsIgnoreCase("CASH")) {
                if (isEarly) {
                    discountAmount = invoice.getSingle_discount_amount();
                } else {
                    discountAmount = invoice.getAmount();
                }
                // For non-card payments, use the original transaction amount
            } else if (invoice.getStatus() == InvoiceStatus.PARTIALLY) {
                discountAmount = invoice.getAmount();
            }


            // Process the transaction based on the payment amount
            if (transactionAmount.compareTo(discountAmount) < 0) {
                return handlePartialPayment(invoice, transaction, transactionDto, transactionAmount);
            } else {
                return handleFullPayment(invoice, transactionDto, transaction, transactionAmount, isEarly, discountAmount);
            }
        } catch (CustomerNotFoundException | InvoiceNotFoundException | InvalidTransactionException e) {
            // Handle known exceptions and return appropriate error responses
            logger.error("Transaction creation failed: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false,e.getMessage()));

        } catch (Exception e) {
            // Handle any unexpected exceptions
            logger.error("Unexpected error during transaction creation: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false,"An unexpected error occurred."));
        }
    }

    private ResponseEntity<?> handlePartialPayment(Invoice invoice, Transaction transaction, TransactionDto transactionDto, BigDecimal transactionAmount) {
        try {
            curr = "partial";

                validateCardDetails(transactionDto);
                // find cardDetails by its cardId
                CardDetails cardDetails = cardDetailsRepo.findById(transactionDto.getCardId())
                        .orElse(null);

                if (cardDetails == null) {
                    return ResponseEntity.ok(new ApiResponse(false, "Card Detail not found."));
                }

                transaction.setMethodId(cardDetails.getId());
                ResponseEntity<ApiResponse> paymentResponse = processPayment(transactionDto, transaction, invoice, transactionAmount, curr);
                if (!paymentResponse.getBody().isSuccess()) {
                    return paymentResponse;
                }

                Transaction savedTransaction = transactionRepo.save(transaction);
            return ResponseEntity.ok(new ApiResponse(true, "Transaction processed successfully for partial payment."));
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
            logger.error("Partial payment transaction failed: {}", e.getMessage());
            return ResponseEntity.ok("Failed to process partial payment: " + e.getMessage());
        }
    }


    private ResponseEntity<?> handleFullPayment(Invoice invoice, TransactionDto transactionDto, Transaction transaction,
                                                BigDecimal transactionAmount, boolean isEarly, BigDecimal discountAmount) {
        try {
            curr = "full";
            // Call the validation method and get the response
            ResponseEntity<ApiResponse> validationResponse = validateFullPayment(invoice, transactionDto, transaction,
                    transactionAmount, isEarly, discountAmount);

            // If validation fails, return the error response
            if (!validationResponse.getBody().isSuccess()) {
                return validationResponse;
            }

            ResponseEntity<ApiResponse> paymentResponse = processPayment(transactionDto, transaction, invoice, discountAmount,curr);

            // If payment fails, return the error response
            if (!paymentResponse.getBody().isSuccess()) {
                return paymentResponse;
            }

            Transaction savedTransaction = transactionRepo.save(transaction);
            return ResponseEntity.ok(new ApiResponse(true, "Transaction saved successfully"));

        } catch (Exception e) {
            logger.error("Transaction failed: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false, "Failed to process full payment: " + e.getMessage()));
        }
    }


    private ResponseEntity<ApiResponse> validateFullPayment(Invoice invoice, TransactionDto transactionDto,
                                                            Transaction transaction, BigDecimal transactionAmount,
                                                            boolean isEarly, BigDecimal discountAmount) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return ResponseEntity.ok(new ApiResponse(false, "Invoice has already been fully paid."));
        }

        if (transactionAmount.compareTo(discountAmount) > 0) {
            // User entered greater amount than the bill amount
            return ResponseEntity.ok(new ApiResponse(false, "Payment amount exceeds the invoice amount."));
        }

        // If validation passes, return a success response
        return ResponseEntity.ok(new ApiResponse(true, "Validation successful."));
    }


    private ResponseEntity<ApiResponse> processPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice, BigDecimal discountAmount, String curr) {
        try {
            // Handle different payment methods and return their respective responses
            switch (transactionDto.getPaymentMethod().toUpperCase()) {
                case "CARD":
                    // Return the response from handleCardPayment
                    return handleCardPayment(transactionDto, transaction, invoice, discountAmount,curr);

                case "WALLET":
                    // Return the response from handleWalletPayment
                    return handleWalletPayment(transactionDto, transaction, invoice, discountAmount,curr);

                case "CASH":
                    // Return the response from handleCashPayment
                    return handleCashPayment(transactionDto, transaction, invoice);

                default:
                    // Return BAD_REQUEST for an invalid payment method
                    return ResponseEntity.ok(new ApiResponse(false, "Invalid payment method."));
            }
        } catch (Exception e) {
            // If any exception occurs, mark the transaction as failed
            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
            transactionRepo.save(transaction);
            logger.error("Payment processing failed: {}", e.getMessage());

            // Return INTERNAL_SERVER_ERROR with the error message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Payment processing failed: " + e.getMessage()));
        }
    }


    private ResponseEntity<ApiResponse> handleCardPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice, BigDecimal discountAmount, String curr) {
        try {
            // Validate card details before proceeding
            ResponseEntity<ApiResponse> validationResponse = validateCardDetails(transactionDto);

            // If validation fails, return the validation response
            if (!validationResponse.getBody().isSuccess()) {
                return validationResponse;
            }
            // Retrieve card details from repository
            CardDetails cardDetails = cardDetailsRepo.findById(transactionDto.getCardId())
                    .orElseThrow(() -> new InvalidTransactionException("Card Detail not found"));

            // Associate the card details with the transaction
            transaction.setMethodId(cardDetails.getId());

            // Set the transaction amount to the discounted amount
            transaction.setAmount(discountAmount);

            // Simulate the card payment
            boolean cardPaymentSuccess = simulateCardPayment(cardDetails, transaction.getAmount());

            // Handle payment success
            if (cardPaymentSuccess) {
                transaction.setStatus("SUCCESS");
                invoice.setAmount(invoice.getAmount().subtract(discountAmount));
                cardDetails.debit(discountAmount);
                if(curr.equalsIgnoreCase("full")){
                    invoice.setStatus(InvoiceStatus.PAID);  // Mark the invoice as fully paid
                    transaction.setInvoice_status("FULL");
                }
                else{
                    invoice.setStatus(InvoiceStatus.PARTIALLY); // Update invoice status
                    invoice.setDouble_discount_amount(BigDecimal.valueOf(0));
                    invoice.setSingle_discount_amount(BigDecimal.valueOf(0));
                    transaction.setInvoice_status("PARTIAL");
                }
                invoiceRepo.save(invoice);
                transactionRepo.save(transaction);

                // Return a successful response
                return ResponseEntity.ok(new ApiResponse(true, "Card payment processed successfully"));
            }
            // Handle payment failure
            else {
                transaction.setStatus("FAILED");
                transaction.setAmount(BigDecimal.valueOf(0));  // Reset transaction amount on failure
                transaction.setInvoice_status("FAILED");
                transactionRepo.save(transaction);

                // Return a failed response
                // For payment-related failures
                return ResponseEntity.ok(new ApiResponse(false, "Card payment declined due to insufficient funds"));

            }

        } catch (InvalidTransactionException e) {
            // Handle invalid transaction errors
            logger.error("Card payment error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            // Handle general exceptions
            logger.error("Card payment processing failed: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false, "An error occurred during card payment processing"));
        }
    }


    private ResponseEntity<ApiResponse> handleWalletPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice, BigDecimal discountAmount, String curr) {
        try {
            // Fetch wallet details
            WalletDetails walletDetails = walletDetailsRepo.findByCustomer(transaction.getCustomer())
                    .orElseThrow(() -> new InvalidTransactionException("Wallet not found for the customer."));

            BigDecimal walletBalance = walletDetails.getBalance();

            // Check if wallet has sufficient balance
            if (walletBalance.compareTo(discountAmount) < 0) {
                return ResponseEntity.ok(new ApiResponse(false, "Insufficient wallet balance."));
            }

            // Debit wallet and save
            walletDetails.debit(transactionDto.getAmount());
            walletDetailsRepo.save(walletDetails);

            // Associate wallet details with transaction
            transaction.setMethodId(walletDetails.getId());
            transaction.setStatus("SUCCESS");
            transaction.setAmount(discountAmount);

            // Update invoice status and amount
            invoice.setAmount(invoice.getAmount().subtract(discountAmount));
            if(curr.equalsIgnoreCase("full")){
                invoice.setStatus(InvoiceStatus.PAID);
                transaction.setInvoice_status("FULL");
            }
            else{
                invoice.setStatus(InvoiceStatus.PARTIALLY);
                transaction.setInvoice_status("PARTIAL");
                invoice.setDouble_discount_amount(BigDecimal.valueOf(0));
                invoice.setSingle_discount_amount(BigDecimal.valueOf(0));
            }
            invoiceRepo.save(invoice);

            // Return success response
            return ResponseEntity.ok(new ApiResponse(true, "Wallet payment processed successfully."));

        } catch (InvalidTransactionException e) {
            logger.error("Wallet payment failed: {}", e.getMessage());
            // Handle invalid transaction (e.g., wallet not found or insufficient balance)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Wallet payment processing failed: {}", e.getMessage());
            // Handle any other unexpected exceptions
            return ResponseEntity.ok(new ApiResponse(false, "Failed to process wallet payment: " + e.getMessage()));
        }
    }


    private ResponseEntity<ApiResponse> handleCashPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice) {
        try {
            // Process the cash payment
            transaction.setStatus("SUCCESS");
            transaction.setAmount(transactionDto.getAmount());
            invoice.setStatus(InvoiceStatus.PAID);
            transaction.setInvoice_status("FULL");

            // Return success response
            return ResponseEntity.ok(new ApiResponse(true, "Cash payment processed successfully."));

        } catch (Exception e) {
            logger.error("Cash payment processing failed: {}", e.getMessage());
            // Handle any unexpected exceptions
            return ResponseEntity.ok(new ApiResponse(false, "Failed to process cash payment: " + e.getMessage()));
        }
    }


    private boolean simulateCardPayment(CardDetails cardDetails, BigDecimal amount) {
        if(cardDetails.getBalance().compareTo(amount) < 0) {
            return false; // Insufficient balance
        }
        return true;
    }


    private ResponseEntity<ApiResponse> validateCardDetails(TransactionDto transactionDto) {
        CardDetails cardDetails = cardDetailsRepo.findById(transactionDto.getCardId())
                .orElseThrow(() -> new InvalidTransactionException("Card details not found."));

        if (cardDetails.getCardNumber() == null || cardDetails.getCardNumber().length() != 16) {
            return ResponseEntity.ok(new ApiResponse(false, "Card number must be 16 digits."));
        }
        if (cardDetails.getCvv() == null || cardDetails.getCvv().length() != 3) {
            return ResponseEntity.ok(new ApiResponse(false, "CVV must be 3 digits."));
        }
        if (!isExpiryDateValid(cardDetails.getExpiryDate())) {
            return ResponseEntity.ok(new ApiResponse(false, "Card expiry date must be in the future."));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Card details are valid."));
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