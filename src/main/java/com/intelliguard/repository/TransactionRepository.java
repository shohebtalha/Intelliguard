package com.intelliguard.repository;

import com.intelliguard.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // Find all transactions by a specific sender
    List<Transaction> findBySenderId(String senderId);

    // Find all transactions with a specific status (APPROVED, BLOCKED, REVIEW)
    List<Transaction> findByStatus(String status);

    // Count how many transactions a sender made after a given time
    // Used by velocity check: "did this person send > 5 txns in last 10 minutes?"
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderId = :senderId AND t.createdAt >= :since")
    long countRecentTransactions(@Param("senderId") String senderId,
                                 @Param("since") LocalDateTime since);

    // Sum total amount sent by a sender after a given time
    // Used to detect: "did they send > ₹1L in last hour?"
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderId = :senderId AND t.createdAt >= :since")
    BigDecimal sumRecentAmount(@Param("senderId") String senderId,
                               @Param("since") LocalDateTime since);

    // Find all blocked transactions (for the fraud alerts screen)
    List<Transaction> findByStatusOrderByCreatedAtDesc(String status);
}