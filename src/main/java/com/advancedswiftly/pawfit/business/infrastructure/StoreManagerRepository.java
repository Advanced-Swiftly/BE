package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StoreManager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreManagerRepository extends JpaRepository<StoreManager, Long> {
}
