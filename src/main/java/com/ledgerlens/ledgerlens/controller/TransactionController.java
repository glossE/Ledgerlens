package com.ledgerlens.ledgerlens.controller;

import com.ledgerlens.ledgerlens.dto.TransactionUploadResponse;
import com.ledgerlens.ledgerlens.model.Transaction;
import com.ledgerlens.ledgerlens.repository.TransactionRepository;
import com.ledgerlens.ledgerlens.service.AnomalyDetectionService;
import com.ledgerlens.ledgerlens.service.CsvParserService;
import com.ledgerlens.ledgerlens.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final CsvParserService csvParserService;
    private final GeminiService geminiService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final TransactionRepository transactionRepository;

    // Constructor injection (Spring injects these beans automatically)
    public TransactionController(CsvParserService csvParserService,
                                 GeminiService geminiService,
                                 AnomalyDetectionService anomalyDetectionService,
                                 TransactionRepository transactionRepository) {
        this.csvParserService = csvParserService;
        this.geminiService = geminiService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<TransactionUploadResponse> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            // Step 1: Parse CSV -> save raw transactions to DB
            List<Transaction> transactions = csvParserService.parseAndSave(file);

            // Step 2: Call Gemini to categorize each transaction
            transactions = geminiService.categorize(transactions);

            // Step 3: Run anomaly detection rules
            transactions = anomalyDetectionService.detect(transactions);

            // Step 4: Persist the updated categories + flags back to DB
            transactions = transactionRepository.saveAll(transactions);

            // Step 5: Build response
            int flaggedCount = (int) transactions.stream().filter(Transaction::getIsFlagged).count();
            TransactionUploadResponse response = new TransactionUploadResponse(
                    transactions.size(), flaggedCount, transactions
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // In production you'd use a proper @ControllerAdvice exception handler
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<Transaction>> getFlaggedTransactions() {
        return ResponseEntity.ok(transactionRepository.findByIsFlaggedTrue());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Transaction>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(transactionRepository.findByCategory(category));
    }
}
