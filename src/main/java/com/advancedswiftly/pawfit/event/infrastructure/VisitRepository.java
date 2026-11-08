package com.advancedswiftly.pawfit.event.infrastructure;

import com.advancedswiftly.pawfit.event.domain.Visit;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    boolean existsByUserIdAndQrTokenHash(Long userId, String qrTokenHash);
    @EntityGraph(attributePaths = {"store"})
    Page<Visit> findAllByUserIdAndVisitedAtBetween(Long userId, Instant from, Instant to, Pageable pageable);
}
