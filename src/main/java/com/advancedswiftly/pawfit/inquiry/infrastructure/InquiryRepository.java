package com.advancedswiftly.pawfit.inquiry.infrastructure;

import com.advancedswiftly.pawfit.inquiry.domain.Inquiry;
import com.advancedswiftly.pawfit.inquiry.domain.InquiryStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findAllByUserId(Long userId, Pageable pageable);

    Page<Inquiry> findAllByUserIdAndStatus(
            Long userId, InquiryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "attachments")
    Optional<Inquiry> findByIdAndUserId(Long inquiryId, Long userId);
}
