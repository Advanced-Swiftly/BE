package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.ConditionRequestStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConditionRequestRepository extends JpaRepository<ConditionRequest, Long> {

    Optional<ConditionRequest> findByIdAndSubmittedBy(Long id, Long submittedBy);

    Page<ConditionRequest> findByStoreIdOrderByIdDesc(Long storeId, Pageable pageable);

    Page<ConditionRequest> findByStoreIdAndStatusOrderByIdDesc(
            Long storeId, ConditionRequestStatus status, Pageable pageable);
}
