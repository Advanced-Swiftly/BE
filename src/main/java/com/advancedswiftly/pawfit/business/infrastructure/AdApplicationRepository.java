package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.AdApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdApplicationRepository extends JpaRepository<AdApplication, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from AdApplication application where application.id = :id")
    Optional<AdApplication> findByIdForUpdate(@Param("id") Long id);

    Optional<AdApplication> findByIdAndSubmittedBy(Long id, Long submittedBy);

    List<AdApplication> findBySubmittedByOrderByIdDesc(Long submittedBy);
}
