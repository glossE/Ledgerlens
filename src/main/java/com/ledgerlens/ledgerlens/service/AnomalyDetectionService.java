package com.ledgerlens.ledgerlens.service;

import com.ledgerlens.ledgerlens.model.Transaction;
import com.ledgerlens.ledgerlens.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnomalyDetectionService {

    private final TransactionRepository transactionRepository;

    public AnomalyDetectionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private static final BigDecimal ROUND_NUMBER_THRESHOLD = new BigDecimal("50000");

    public List<Transaction> detect(List<Transaction> transactions) {
        flagDuplicates(transactions);
        flagRoundNumbers(transactions);
        flagSpendSpikes(transactions);
        return transactions;
    }

    // Flag: same description + same amount within 48 hours of another transaction in this batch
    private void flagDuplicates(List<Transaction> transactions) {
        for (int i = 0; i < transactions.size(); i++) {
            for (int j = i + 1; j < transactions.size(); j++) {
                Transaction a = transactions.get(i);
                Transaction b = transactions.get(j);

                boolean sameAmount = a.getAmount().compareTo(b.getAmount()) == 0;
                boolean sameVendor = a.getDescription().equalsIgnoreCase(b.getDescription());
                boolean within48hrs = Math.abs(
                        a.getTransactionDate().toEpochDay() - b.getTransactionDate().toEpochDay()
                ) <= 2;

                if (sameAmount && sameVendor && within48hrs) {
                    flag(a, "Duplicate payment: same amount and vendor within 48 hours");
                    flag(b, "Duplicate payment: same amount and vendor within 48 hours");
                }
            }
        }
    }

    // Flag: DEBIT transactions >= 50,000 where amount is a multiple of 1000 (e.g. 75000, 100000)
    private void flagRoundNumbers(List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            if (!"DEBIT".equalsIgnoreCase(tx.getType())) continue;
            if (tx.getAmount().compareTo(ROUND_NUMBER_THRESHOLD) < 0) continue;

            // Check if the amount has no paise and is a multiple of 1000
            BigDecimal[] divAndRem = tx.getAmount().divideAndRemainder(new BigDecimal("1000"));
            if (divAndRem[1].compareTo(BigDecimal.ZERO) == 0) {
                flag(tx, "Round-number transaction >= 50,000 - possible fabricated invoice");
            }
        }
    }

    // Flag: vendor's total spend in this batch > 200% of their historical monthly average
    private void flagSpendSpikes(List<Transaction> transactions) {
        // Group current batch debits by vendor
        Map<String, BigDecimal> batchSpendByVendor = transactions.stream()
                .filter(tx -> "DEBIT".equalsIgnoreCase(tx.getType()))
                .collect(Collectors.groupingBy(
                        tx -> tx.getDescription().toLowerCase(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        // Compare against historical data already in the DB
        List<Transaction> historical = transactionRepository.findByType("DEBIT");

        Map<String, BigDecimal> historicalTotalByVendor = historical.stream()
                .collect(Collectors.groupingBy(
                        tx -> tx.getDescription().toLowerCase(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Map<String, Long> historicalMonthsByVendor = historical.stream()
                .collect(Collectors.groupingBy(
                        tx -> tx.getDescription().toLowerCase(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        tx -> tx.getTransactionDate().getYear() * 100 + tx.getTransactionDate().getMonthValue(),
                                        Collectors.toSet()
                                ),
                                set -> (long) set.size()
                        )
                ));

        for (Map.Entry<String, BigDecimal> entry : batchSpendByVendor.entrySet()) {
            String vendor = entry.getKey();
            BigDecimal batchSpend = entry.getValue();

            BigDecimal historicalTotal = historicalTotalByVendor.getOrDefault(vendor, BigDecimal.ZERO);
            long months = historicalMonthsByVendor.getOrDefault(vendor, 0L);

            if (months == 0) continue; // No history - can't determine a spike

            BigDecimal monthlyAverage = historicalTotal.divide(new BigDecimal(months), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal threshold = monthlyAverage.multiply(new BigDecimal("2.0"));

            if (batchSpend.compareTo(threshold) > 0) {
                transactions.stream()
                        .filter(tx -> tx.getDescription().equalsIgnoreCase(vendor) && "DEBIT".equalsIgnoreCase(tx.getType()))
                        .forEach(tx -> flag(tx, "Spend spike: vendor spend >200% of monthly average (avg: " + monthlyAverage + ")"));
            }
        }
    }

    private void flag(Transaction tx, String reason) {
        tx.setIsFlagged(true);
        // Append reasons if already flagged by a different rule
        if (tx.getFlagReason() == null || tx.getFlagReason().isBlank()) {
            tx.setFlagReason(reason);
        } else {
            tx.setFlagReason(tx.getFlagReason() + " | " + reason);
        }
    }
}
