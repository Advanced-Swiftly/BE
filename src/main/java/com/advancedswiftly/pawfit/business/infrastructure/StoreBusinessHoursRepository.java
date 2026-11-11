package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StoreBusinessHours;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreBusinessHoursRepository extends JpaRepository<StoreBusinessHours, Long> {

    List<StoreBusinessHours> findByStoreId(Long storeId);
}
