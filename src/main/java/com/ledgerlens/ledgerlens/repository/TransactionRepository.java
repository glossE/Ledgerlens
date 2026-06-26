package com.ledgerlens.ledgerlens.repository;

import com.ledgerlens.ledgerlens.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByIsFlaggedTrue();

    List<Transaction> findByCategory(String category);

    List<Transaction> findByType(String type);
}
