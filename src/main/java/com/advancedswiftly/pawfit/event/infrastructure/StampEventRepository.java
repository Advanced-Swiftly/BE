package com.advancedswiftly.pawfit.event.infrastructure;

import com.advancedswiftly.pawfit.event.domain.EventStatus;
import com.advancedswiftly.pawfit.event.domain.StampEvent;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StampEventRepository extends JpaRepository<StampEvent, Long> {
    @EntityGraph(attributePaths = {"regionIds", "prizes"})
    Optional<StampEvent> findDetailById(Long eventId);

    @EntityGraph(attributePaths = {"regionIds"})
    Optional<StampEvent> findFirstByStatusAndStartsOnLessThanEqualAndEndsOnGreaterThanEqualOrderByStartsOnDesc(
            EventStatus status, LocalDate startsOn, LocalDate endsOn);
}
