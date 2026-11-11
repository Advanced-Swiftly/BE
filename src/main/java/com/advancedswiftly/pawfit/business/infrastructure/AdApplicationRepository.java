package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdApplicationRepository extends JpaRepository<AdApplication, Long> {

    Optional<AdApplication> findByIdAndSubmittedBy(Long id, Long submittedBy);

    List<AdApplication> findBySubmittedByOrderByIdDesc(Long submittedBy);
}
