package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConditionRequestRepository extends JpaRepository<ConditionRequest, Long> {

    Optional<ConditionRequest> findByIdAndSubmittedBy(Long id, Long submittedBy);
}
