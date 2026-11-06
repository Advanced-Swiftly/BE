package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
