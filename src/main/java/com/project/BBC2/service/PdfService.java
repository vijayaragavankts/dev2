package com.project.BBC2.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPCell;
import com.project.BBC2.model.Transaction;

import com.project.BBC2.repository.TransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfService {

    @Autowired
    private TransactionRepo transactionRepo;

    // Define styles for fonts
    private Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
    private Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
    private Font discountFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC);

    public byte[] generatePdf(Long billId) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        Transaction transaction = transactionRepo.findByInvoiceIdAndStatus(billId, "SUCCESS"); // Replace with actual logic to fetch transaction

        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found for the provided bill ID");
        }

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("Payment Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Bharat Bijili Corporation (BBC)", normalFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph("Customer Service: +91 12345 67890 | Email: support@bbc.com", normalFont));
            document.add(new Paragraph(" ")); // Add space


            PdfPTable paymentDetailsTable = new PdfPTable(2);
            paymentDetailsTable.setWidthPercentage(100);
            addCell(paymentDetailsTable, "Transaction ID:", headerFont);
            addCell(paymentDetailsTable, String.valueOf(transaction.getTransactionId()), normalFont);
            addCell(paymentDetailsTable, "Customer ID:", headerFont);
            addCell(paymentDetailsTable, transaction.getCustomer().getCustomerId(), normalFont);
            addCell(paymentDetailsTable, "Customer Name:", headerFont);
            addCell(paymentDetailsTable, transaction.getCustomer().getName(), normalFont);
            addCell(paymentDetailsTable, "Customer Email:", headerFont);
            addCell(paymentDetailsTable, transaction.getCustomer().getEmail(), normalFont);
            addCell(paymentDetailsTable, "Customer Phone:", headerFont);
            addCell(paymentDetailsTable, transaction.getCustomer().getPhoneNumber(), normalFont);
            addCell(paymentDetailsTable, "Bill ID:", headerFont);
            addCell(paymentDetailsTable, String.valueOf(transaction.getInvoice().getInvoice_id()), normalFont);
            addCell(paymentDetailsTable, "Units Consumed:", headerFont);
            addCell(paymentDetailsTable, String.valueOf(transaction.getInvoice().getUnit_consumed()), normalFont);
            addCell(paymentDetailsTable, "Bill Due Date:", headerFont);
            addCell(paymentDetailsTable, transaction.getInvoice().getDue_date().toString(), normalFont);
            addCell(paymentDetailsTable, "Original Amount:", headerFont);
            addCell(paymentDetailsTable, transaction.getInvoice().getAmount().toString(), normalFont);
            addCell(paymentDetailsTable, "Payment Method:", headerFont);
            addCell(paymentDetailsTable, transaction.getPaymentMethod(), normalFont);
            addCell(paymentDetailsTable, "Paid:", headerFont);
            addCell(paymentDetailsTable, transaction.getStatus(), normalFont);
            addCell(paymentDetailsTable, "Early Payment:", headerFont);
            addCell(paymentDetailsTable, transaction.getIsEarly() != null && transaction.getIsEarly() ? "Yes" : "No", normalFont);
            addCell(paymentDetailsTable, "Paid Online:", headerFont);
            addCell(paymentDetailsTable, transaction.getPaymentMethod().equals("ONLINE") ? "Yes" : "No", normalFont);
            addCell(paymentDetailsTable, "Amount Paid:", headerFont);
            addCell(paymentDetailsTable, transaction.getAmount().toString(), normalFont);
            addCell(paymentDetailsTable, "Payment Date:", headerFont);
            addCell(paymentDetailsTable, transaction.getTransactionDate().toString(), normalFont);

            document.add(paymentDetailsTable);


            document.add(new Paragraph("Thank you for your payment!", normalFont));
        } finally {
            document.close();
        }

        return out.toByteArray();
    }


    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(8);
        table.addCell(cell);
    }
}
