package com.advancedswiftly.pawfit.auth.infrastructure;

import com.advancedswiftly.pawfit.auth.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.loginId = :loginId")
    Optional<User> findByLoginIdForUpdate(@Param("loginId") String loginId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumberForUpdate(@Param("phoneNumber") String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
