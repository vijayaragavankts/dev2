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



                if (isEarly) {

                    discountAmount = invoice.getDouble_discount_amount();
                } else {

                    discountAmount = invoice.getSingle_discount_amount();
                }

            } else if (invoice.getStatus() != InvoiceStatus.PARTIALLY && transactionDto.getPaymentMethod().equalsIgnoreCase("CASH")) {
                if (isEarly) {
                    discountAmount = invoice.getSingle_discount_amount();
                } else {
                    discountAmount = invoice.getAmount();
                }

            } else if (invoice.getStatus() == InvoiceStatus.PARTIALLY) {
                discountAmount = invoice.getAmount();
            }

            if (transactionAmount.compareTo(discountAmount) < 0) {
                return handlePartialPayment(invoice, transaction, transactionDto, transactionAmount);
            } else {
                return handleFullPayment(invoice, transactionDto, transaction, transactionAmount, isEarly, discountAmount);
            }
        } catch (CustomerNotFoundException | InvoiceNotFoundException | InvalidTransactionException e) {

            logger.error("Transaction creation failed: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false,e.getMessage()));

        } catch (Exception e) {

            logger.error("Unexpected error during transaction creation: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false,"An unexpected error occurred."));
        }
    }

    private ResponseEntity<?> handlePartialPayment(Invoice invoice, Transaction transaction, TransactionDto transactionDto, BigDecimal transactionAmount) {
        try {
            curr = "partial";

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

            ResponseEntity<ApiResponse> validationResponse = validateFullPayment(invoice, transactionDto, transaction,
                    transactionAmount, isEarly, discountAmount);


            if (!validationResponse.getBody().isSuccess()) {
                return validationResponse;
            }

            ResponseEntity<ApiResponse> paymentResponse = processPayment(transactionDto, transaction, invoice, discountAmount,curr);


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

            return ResponseEntity.ok(new ApiResponse(false, "Payment amount exceeds the invoice amount."));
        }


        return ResponseEntity.ok(new ApiResponse(true, "Validation successful."));
    }


    private ResponseEntity<ApiResponse> processPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice, BigDecimal discountAmount, String curr) {
        try {

            switch (transactionDto.getPaymentMethod().toUpperCase()) {
                case "CARD":

                    return handleCardPayment(transactionDto, transaction, invoice, discountAmount,curr);

                case "WALLET":

                    return handleWalletPayment(transactionDto, transaction, invoice, discountAmount,curr);

                case "CASH":

                    return handleCashPayment(transactionDto, transaction, invoice);

                default:

                    return ResponseEntity.ok(new ApiResponse(false, "Invalid payment method."));
            }
        } catch (Exception e) {

            transaction.setStatus("FAILED");
            transaction.setInvoice_status("FAILED");
            transactionRepo.save(transaction);
            logger.error("Payment processing failed: {}", e.getMessage());


            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Payment processing failed: " + e.getMessage()));
        }
    }


    private ResponseEntity<ApiResponse> handleCardPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice, BigDecimal discountAmount, String curr) {
        try {

            ResponseEntity<ApiResponse> validationResponse = validateCardDetails(transactionDto);


            if (!validationResponse.getBody().isSuccess()) {
                return validationResponse;
            }

            CardDetails cardDetails = cardDetailsRepo.findById(transactionDto.getCardId())
                    .orElseThrow(() -> new InvalidTransactionException("Card Detail not found"));


            transaction.setMethodId(cardDetails.getId());


            transaction.setAmount(discountAmount);

            if(curr.equalsIgnoreCase("partial")){
                BigDecimal minimumPaymentThreshold = invoice.getAmount().multiply(BigDecimal.valueOf(0.20)); //20% threhold


                if (discountAmount.compareTo(minimumPaymentThreshold) < 0) {
                    return ResponseEntity.ok(new ApiResponse(false, "Payment amount must be at least 20% of the total invoice amount."));
                }
            }


            boolean cardPaymentSuccess = simulateCardPayment(cardDetails, transaction.getAmount());


            if (cardPaymentSuccess) {
                transaction.setStatus("SUCCESS");
                invoice.setAmount(invoice.getAmount().subtract(discountAmount));
                cardDetails.debit(discountAmount);
                if(curr.equalsIgnoreCase("full")){
                    invoice.setStatus(InvoiceStatus.PAID);
                    transaction.setInvoice_status("FULL");
                }
                else{
                    invoice.setStatus(InvoiceStatus.PARTIALLY);
                    invoice.setDouble_discount_amount(BigDecimal.valueOf(0));
                    invoice.setSingle_discount_amount(BigDecimal.valueOf(0));
                    transaction.setInvoice_status("PARTIAL");
                }
                invoiceRepo.save(invoice);
                transactionRepo.save(transaction);


                return ResponseEntity.ok(new ApiResponse(true, "Card payment processed successfully"));
            }

            else {
                transaction.setStatus("FAILED");
                transaction.setAmount(BigDecimal.valueOf(0));  // Reset transaction amount on failure
                transaction.setInvoice_status("FAILED");
                transactionRepo.save(transaction);


                return ResponseEntity.ok(new ApiResponse(false, "Card payment declined due to insufficient funds"));

            }

        } catch (InvalidTransactionException e) {

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

            WalletDetails walletDetails = walletDetailsRepo.findByCustomer(transaction.getCustomer())
                    .orElseThrow(() -> new InvalidTransactionException("Wallet not found for the customer."));

            if (walletDetails == null) {
                return ResponseEntity.ok(new ApiResponse(false, "Wallet details not found for the customer."));
            }

            transaction.setMethodId(walletDetails.getId());
            transaction.setAmount(discountAmount);

            BigDecimal walletBalance = walletDetails.getBalance();

            if(curr.equalsIgnoreCase("partial")){

                BigDecimal minimumPaymentThreshold = invoice.getAmount().multiply(BigDecimal.valueOf(0.20)); // 20% threshold


                if (discountAmount.compareTo(minimumPaymentThreshold) < 0) {
                    return ResponseEntity.ok(new ApiResponse(false, "Payment amount must be at least 20% of the total invoice amount."));
                }
            }



            boolean walletPaymentSuccess = simulateWalletPayment(walletDetails, discountAmount);
            if (walletPaymentSuccess) {
                System.out.println("Executed success wallet ==============================================================================================");
                transaction.setStatus("SUCCESS");
                invoice.setAmount(invoice.getAmount().subtract(discountAmount));
                walletDetails.debit(discountAmount);
                if (curr.equalsIgnoreCase("full")) {
                    invoice.setStatus(InvoiceStatus.PAID);
                    transaction.setInvoice_status("FULL");
                } else {
                    invoice.setStatus(InvoiceStatus.PARTIALLY);
                    invoice.setDouble_discount_amount(BigDecimal.valueOf(0));
                    invoice.setSingle_discount_amount(BigDecimal.valueOf(0));
                    transaction.setInvoice_status("PARTIAL");
                }
                invoiceRepo.save(invoice);
                transactionRepo.save(transaction);

                return ResponseEntity.ok(new ApiResponse(true, "Wallet payment processed successfully"));
            } else {
                System.out.println("Executed error wallet ==============================================================================================");

                transaction.setStatus("FAILED");
                transaction.setAmount(BigDecimal.valueOf(0));
                transaction.setInvoice_status("FAILED");
                transactionRepo.save(transaction);


                return ResponseEntity.ok(new ApiResponse(false, "Wallet payment declined due to insufficient funds"));

            }

        } catch (InvalidTransactionException e) {
            logger.error("Wallet payment failed: {}", e.getMessage());

            return ResponseEntity.ok(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Wallet payment processing failed: {}", e.getMessage());

            return ResponseEntity.ok(new ApiResponse(false, "Failed to process wallet payment: " + e.getMessage()));
        }
    }


    private ResponseEntity<ApiResponse> handleCashPayment(TransactionDto transactionDto, Transaction transaction, Invoice invoice) {
        try {

            transaction.setStatus("SUCCESS");
            transaction.setAmount(transactionDto.getAmount());
            invoice.setStatus(InvoiceStatus.PAID);
            transaction.setInvoice_status("FULL");


            return ResponseEntity.ok(new ApiResponse(true, "Cash payment processed successfully."));

        } catch (Exception e) {
            logger.error("Cash payment processing failed: {}", e.getMessage());

            return ResponseEntity.ok(new ApiResponse(false, "Failed to process cash payment: " + e.getMessage()));
        }
    }


    private boolean simulateCardPayment(CardDetails cardDetails, BigDecimal amount) {
        if(cardDetails.getBalance().compareTo(amount) < 0) {
            return false;
        }
        return true;
    }

    public boolean simulateWalletPayment(WalletDetails walletDetails, BigDecimal amount){
        if(walletDetails.getBalance().compareTo(amount) < 0) {
            return false;
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