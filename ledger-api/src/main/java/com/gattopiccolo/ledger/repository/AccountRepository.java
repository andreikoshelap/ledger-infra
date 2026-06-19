package com.gattopiccolo.ledger.repository;

import com.gattopiccolo.ledger.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Pessimistic write lock (SELECT ... FOR UPDATE). Used inside the debit/exchange
     * critical section so concurrent money mutations on the same account serialise
     * and cannot race past the funds check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    List<Account> findByUserIdOrderByIdAsc(Long userId);
}
