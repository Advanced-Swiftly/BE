package com.advancedswiftly.pawfit.event.infrastructure;

import com.advancedswiftly.pawfit.event.domain.EventStamp;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStampRepository extends JpaRepository<EventStamp, Long> {
    boolean existsByEventIdAndUserIdAndStoreId(Long eventId, Long userId, Long storeId);
    long countByEventIdAndUserId(Long eventId, Long userId);
    @EntityGraph(attributePaths = {"store"})
    List<EventStamp> findAllByEventIdAndUserId(Long eventId, Long userId);
}
