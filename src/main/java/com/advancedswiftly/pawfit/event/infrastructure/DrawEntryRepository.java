package com.advancedswiftly.pawfit.event.infrastructure;

import com.advancedswiftly.pawfit.event.domain.DrawEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    @EntityGraph(attributePaths = {"prize"})
    Optional<DrawEntry> findByEventIdAndUserId(Long eventId, Long userId);
}
