package com.ledgerlens.ledgerlens.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate transactionDate;

    private String description;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private String type;

    private String category;

    private Boolean isFlagged = false;

    private String flagReason;

    @Column(columnDefinition = "TEXT")
    private String rawCsvRow;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
