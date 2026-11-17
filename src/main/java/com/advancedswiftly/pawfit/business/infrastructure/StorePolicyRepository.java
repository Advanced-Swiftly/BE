package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StorePolicy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorePolicyRepository extends JpaRepository<StorePolicy, Long> {

    java.util.List<StorePolicy> findByStoreIdInOrderByVersionDesc(
            java.util.Collection<Long> storeIds);

    default Optional<StorePolicy> findActive(Long storeId) {
        return findByStoreIdInOrderByVersionDesc(java.util.List.of(storeId)).stream()
                .filter(
                        p ->
                                p.getConditions() == null
                                        || p.getConditions()
                                                .activeOn(
                                                        com.advancedswiftly.pawfit.business.domain
                                                                .EntryConditions.today()))
                .findFirst();
    }

    /** version이 가장 큰 정책이 현재 정책이다. */
    Optional<StorePolicy> findFirstByStoreIdOrderByVersionDesc(Long storeId);

    Page<StorePolicy> findByStoreIdOrderByVersionDesc(Long storeId, Pageable pageable);
}
