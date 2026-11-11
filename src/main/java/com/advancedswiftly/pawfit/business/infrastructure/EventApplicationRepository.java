package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.ApplicationStatus;
import com.advancedswiftly.pawfit.business.domain.EventApplication;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApplicationRepository extends JpaRepository<EventApplication, Long> {

    boolean existsByStoreIdAndEventIdAndStatusNotIn(
            Long storeId, Long eventId, Collection<ApplicationStatus> statuses);

    Optional<EventApplication> findByIdAndSubmittedBy(Long id, Long submittedBy);

    List<EventApplication> findBySubmittedByOrderByIdDesc(Long submittedBy);
}
