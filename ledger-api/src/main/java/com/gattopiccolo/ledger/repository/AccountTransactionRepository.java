package com.gattopiccolo.ledger.repository;

import com.gattopiccolo.ledger.domain.AccountTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    // First page: newest first.
    List<AccountTransaction> findByAccountIdOrderByIdDesc(Long accountId, Pageable pageable);

    // Subsequent pages via seek pagination on the monotonic id (stable under inserts).
    List<AccountTransaction> findByAccountIdAndIdLessThanOrderByIdDesc(Long accountId, Long cursor, Pageable pageable);
}
