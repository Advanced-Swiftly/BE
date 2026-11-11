package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreVerificationRepository extends JpaRepository<StoreVerification, Long> {

    boolean existsByBusinessNumberAndStatusNot(String businessNumber, VerificationStatus status);

    /** 본인이 인증을 요청해 승인(APPROVED)된 업체인지 확인한다. */
    boolean existsByStoreIdAndSubmittedByAndStatus(Long storeId, Long submittedBy, VerificationStatus status);

    Optional<StoreVerification> findByIdAndSubmittedBy(Long id, Long submittedBy);

    List<StoreVerification> findBySubmittedByOrderByIdDesc(Long submittedBy);

    List<StoreVerification> findBySubmittedByAndStatusOrderByIdDesc(
            Long submittedBy, VerificationStatus status);
}
