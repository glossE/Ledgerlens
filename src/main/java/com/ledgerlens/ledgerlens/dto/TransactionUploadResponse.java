package com.ledgerlens.ledgerlens.dto;

import com.ledgerlens.ledgerlens.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUploadResponse {

    private int totalTransactions;
    private int flaggedCount;
    private List<Transaction> transactions;
}
