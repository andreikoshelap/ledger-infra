package com.gattopiccolo.ledger.repository;

import com.gattopiccolo.ledger.domain.AccountTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    // Single entry scoped to its account, so a foreign account's id cannot be probed.
    Optional<AccountTransaction> findByIdAndAccountId(Long id, Long accountId);

    // First page: newest first.
    List<AccountTransaction> findByAccountIdOrderByIdDesc(Long accountId, Pageable pageable);

    // Subsequent pages via seek pagination on the monotonic id (stable under inserts).
    List<AccountTransaction> findByAccountIdAndIdLessThanOrderByIdDesc(Long accountId, Long cursor, Pageable pageable);
}
