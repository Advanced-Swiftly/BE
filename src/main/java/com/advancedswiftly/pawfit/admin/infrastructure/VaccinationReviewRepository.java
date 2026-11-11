package com.advancedswiftly.pawfit.admin.infrastructure;

import com.advancedswiftly.pawfit.admin.domain.VaccinationReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccinationReviewRepository extends JpaRepository<VaccinationReview, Long> {
}
