package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.Store;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Store s where s.id=:id")
    java.util.Optional<Store> lockById(
            @org.springframework.data.repository.query.Param("id") Long id);
}
