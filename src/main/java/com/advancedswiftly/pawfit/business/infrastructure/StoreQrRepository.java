package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreQrRepository extends JpaRepository<StoreQr, Long> {

    /** 아직 폐기되지 않은 QR 목록. 재발급할 때 모두 폐기한다. */
    List<StoreQr> findByStoreIdAndStatusNot(Long storeId, QrStatus status);

    /** 가장 최근에 발급된 QR이 현재 QR이다. */
    Optional<StoreQr> findFirstByStoreIdOrderByIdDesc(Long storeId);
}
