package com.project.BBC2.controller;

import com.itextpdf.text.DocumentException;
import com.project.BBC2.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/pdf")
@CrossOrigin(origins = "http://localhost:4200")

public class PdfController {

    @Autowired
    private PdfService pdfService;

    @GetMapping("/{billId}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long billId) throws DocumentException, IOException {
        byte[] pdfContent = pdfService.generatePdf(billId);

        String fileName = "invoice_" + billId + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + fileName);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}
