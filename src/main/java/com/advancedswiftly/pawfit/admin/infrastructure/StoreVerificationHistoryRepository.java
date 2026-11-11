package com.advancedswiftly.pawfit.admin.infrastructure;

import com.advancedswiftly.pawfit.admin.domain.StoreVerificationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreVerificationHistoryRepository extends JpaRepository<StoreVerificationHistory, Long> {

    List<StoreVerificationHistory> findByVerificationIdOrderByIdAsc(Long verificationId);
}
