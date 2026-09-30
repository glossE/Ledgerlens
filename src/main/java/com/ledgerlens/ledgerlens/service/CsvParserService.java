package com.ledgerlens.ledgerlens.service;

import com.ledgerlens.ledgerlens.model.Transaction;
import com.ledgerlens.ledgerlens.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParserService {

    private final TransactionRepository transactionRepository;

    public CsvParserService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Expected CSV columns: Date, Description, Amount, Type
    public List<Transaction> parseAndSave(MultipartFile file) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                line = line.trim();
                if (line.isBlank()) continue;

                String[] columns = line.split(",", -1);
                if (columns.length < 4) continue;

                Transaction tx = new Transaction();
                tx.setTransactionDate(LocalDate.parse(columns[0].trim(), DATE_FORMAT));
                tx.setDescription(columns[1].trim());
                tx.setAmount(new BigDecimal(columns[2].trim()));
                tx.setType(columns[3].trim());
                tx.setRawCsvRow(line);

                transactions.add(tx);
            }
        }

        return transactionRepository.saveAll(transactions);
    }
}
