package com.ledgerlens.ledgerlens.dto;

import com.ledgerlens.ledgerlens.model.Transaction;

import java.util.List;

public class TransactionUploadResponse {

    private int totalTransactions;
    private int flaggedCount;
    private List<Transaction> transactions;

    public TransactionUploadResponse() {
    }

    public TransactionUploadResponse(int totalTransactions, int flaggedCount, List<Transaction> transactions) {
        this.totalTransactions = totalTransactions;
        this.flaggedCount = flaggedCount;
        this.transactions = transactions;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public int getFlaggedCount() {
        return flaggedCount;
    }

    public void setFlaggedCount(int flaggedCount) {
        this.flaggedCount = flaggedCount;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
