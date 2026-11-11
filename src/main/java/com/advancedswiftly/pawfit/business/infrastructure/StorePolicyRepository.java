package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorePolicyRepository extends JpaRepository<StorePolicy, Long> {

    /** version이 가장 큰 정책이 현재 정책이다. */
    Optional<StorePolicy> findFirstByStoreIdOrderByVersionDesc(Long storeId);
}
