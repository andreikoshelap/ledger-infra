package com.gattopiccolo.ledger.threat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    /** The active finding that a repeated detection should be folded into, if any. */
    Optional<Finding> findFirstByFingerprintAndStatusInAndLastSeenAfterOrderByLastSeenDesc(
            String fingerprint, Collection<FindingStatus> statuses, Instant after);

    List<Finding> findAllByOrderByRiskScoreDescLastSeenDesc();

    List<Finding> findByStatusOrderByRiskScoreDescLastSeenDesc(FindingStatus status);

    long countByStatusIn(Collection<FindingStatus> statuses);
}
