package com.centsible.backend.controller;

import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.service.TransactionImportService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class TransactionController {

    private final TransactionImportService transactionImportService;

    public TransactionController(TransactionImportService transactionImportService) {
        this.transactionImportService = transactionImportService;
    }

    // POST http://localhost:8080/api/transactions/upload  (multipart/form-data, field name "file")
    @PostMapping("/upload")
    public ImportResultDTO upload(@RequestParam("file") MultipartFile file) {
        return transactionImportService.importCsv(file);
    }
}
