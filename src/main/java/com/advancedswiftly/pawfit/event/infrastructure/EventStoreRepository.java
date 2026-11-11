package com.advancedswiftly.pawfit.event.infrastructure;

import com.advancedswiftly.pawfit.event.domain.EventStore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStoreRepository extends JpaRepository<EventStore, Long> {
    @EntityGraph(attributePaths = {"event", "region"})
    Optional<EventStore> findByQrTokenHash(String qrTokenHash);
    List<EventStore> findAllByEventIdAndApprovedTrueOrderById(Long eventId);
    List<EventStore> findAllByEventIdAndRegionIdAndApprovedTrueOrderById(Long eventId, Long regionId);
}
