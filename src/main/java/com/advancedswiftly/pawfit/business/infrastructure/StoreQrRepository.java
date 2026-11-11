package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StoreQr;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreQrRepository extends JpaRepository<StoreQr, Long> {

    /** 가장 최근에 발급된 QR이 현재 QR이다. */
    Optional<StoreQr> findFirstByStoreIdOrderByIdDesc(Long storeId);
}
