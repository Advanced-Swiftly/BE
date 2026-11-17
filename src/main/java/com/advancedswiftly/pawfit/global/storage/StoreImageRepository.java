package com.advancedswiftly.pawfit.global.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {
    List<StoreImage> findByStoreIdOrderByDisplayOrderAscIdAsc(Long storeId);
}
