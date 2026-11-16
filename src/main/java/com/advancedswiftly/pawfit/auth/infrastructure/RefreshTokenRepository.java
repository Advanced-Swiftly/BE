package com.advancedswiftly.pawfit.auth.infrastructure;

import com.advancedswiftly.pawfit.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select token.user.id from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token join fetch token.user where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);
}
