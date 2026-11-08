package com.advancedswiftly.pawfit.auth.infrastructure;

import com.advancedswiftly.pawfit.auth.domain.PhoneVerification;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant createdAfter);

    Optional<PhoneVerification> findTopByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from PhoneVerification verification where verification.id = :id")
    Optional<PhoneVerification> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PhoneVerification> findByVerificationTokenHash(String verificationTokenHash);
}
