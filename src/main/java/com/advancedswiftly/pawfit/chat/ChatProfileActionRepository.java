package com.advancedswiftly.pawfit.chat;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatProfileActionRepository extends JpaRepository<ChatProfileAction, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ChatProfileAction a where a.id=:id")
    Optional<ChatProfileAction> lock(@Param("id") UUID id);
}
