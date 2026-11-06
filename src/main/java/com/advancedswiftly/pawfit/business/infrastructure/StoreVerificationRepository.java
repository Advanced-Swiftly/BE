package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreVerificationRepository extends JpaRepository<StoreVerification, Long> {

    boolean existsByBusinessNumberAndStatusNot(String businessNumber, VerificationStatus status);
}
