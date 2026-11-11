package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.ManagedStoreListItem;
import com.advancedswiftly.pawfit.business.api.dto.ManagedStoreListResponse;
import com.advancedswiftly.pawfit.business.api.dto.QrIssueRequest;
import com.advancedswiftly.pawfit.business.api.dto.QrIssueResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreDetailResponse;
import com.advancedswiftly.pawfit.business.api.dto.StorePolicyResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreProfileUpdateRequest;
import com.advancedswiftly.pawfit.business.api.dto.StoreProfileUpdateResponse;
import com.advancedswiftly.pawfit.business.api.dto.StoreQrResponse;
import com.advancedswiftly.pawfit.business.domain.QrStatus;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import com.advancedswiftly.pawfit.business.domain.StoreVerification;
import com.advancedswiftly.pawfit.business.domain.VerificationStatus;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreVerificationRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리 업체의 기본 정보, 현재 입장 정책, QR을 다룬다. */
@Service
public class StoreOperationService {

    private final StoreRepository storeRepository;
    private final StorePolicyRepository policyRepository;
    private final StoreQrRepository qrRepository;
    private final ManagedStoreChecker managedStoreChecker;
    private final StoreVerificationRepository verificationRepository;
    private final BusinessHoursService businessHoursService;
    private final EventStoreSynchronizer eventStoreSynchronizer;
    private final SecureRandom secureRandom = new SecureRandom();

    public StoreOperationService(
            StoreRepository storeRepository,
            StorePolicyRepository policyRepository,
            StoreQrRepository qrRepository,
            ManagedStoreChecker managedStoreChecker,
            StoreVerificationRepository verificationRepository,
            BusinessHoursService businessHoursService,
            EventStoreSynchronizer eventStoreSynchronizer) {
        this.verificationRepository = verificationRepository;
        this.businessHoursService = businessHoursService;
        this.eventStoreSynchronizer = eventStoreSynchronizer;
        this.storeRepository = storeRepository;
        this.policyRepository = policyRepository;
        this.qrRepository = qrRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    @Transactional
    public StoreProfileUpdateResponse updateProfile(
            Long userId, Long storeId, StoreProfileUpdateRequest request) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        store.updateProfile(request.name(), request.phoneNumber(), request.description());
        if (request.amenities() != null) {
            store.replaceAmenities(request.amenities());
        }
        return StoreProfileUpdateResponse.from(store);
    }

    /** 현재 정책(가장 큰 version)을 조회한다. 아직 정책이 없는 업체는 404. */
    @Transactional(readOnly = true)
    public StorePolicyResponse getCurrentPolicy(Long userId, Long storeId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        return policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(StorePolicyResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public StoreQrResponse getQr(Long userId, Long storeId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        return StoreQrResponse.withImage(currentQr(storeId));
    }

    @Transactional
    public StoreQrResponse changeQrActive(Long userId, Long storeId, boolean active) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        StoreQr qr = currentQr(storeId);
        if (qr.getStatus() == QrStatus.REVOKED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_STATE, "폐기된 QR은 다시 활성화할 수 없습니다.");
        }
        qr.changeActive(active);
        eventStoreSynchronizer.syncRevoked(storeId, !active);
        return StoreQrResponse.statusOnly(qr);
    }

    private StoreQr currentQr(Long storeId) {
        return qrRepository
                .findFirstByStoreIdOrderByIdDesc(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** 승인된 인증 요청이 있는 내 업체 목록. 같은 업체는 한 번만 담는다. */
    @Transactional(readOnly = true)
    public ManagedStoreListResponse listManagedStores(Long userId) {
        Map<Long, Store> stores = new LinkedHashMap<>();
        for (StoreVerification verification : verificationRepository
                .findBySubmittedByAndStatusOrderByIdDesc(userId, VerificationStatus.APPROVED)) {
            Store store = verification.getStore();
            stores.putIfAbsent(store.getId(), store);
        }
        return new ManagedStoreListResponse(
                stores.values().stream().map(ManagedStoreListItem::from).toList());
    }

    @Transactional(readOnly = true)
    public StoreDetailResponse getStore(Long userId, Long storeId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Integer policyVersion = policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(StorePolicy::getVersion)
                .orElse(null);
        QrStatus qrStatus = qrRepository
                .findFirstByStoreIdOrderByIdDesc(storeId)
                .map(StoreQr::getStatus)
                .orElse(null);
        return new StoreDetailResponse(
                store.getId(),
                store.getName(),
                store.getPhone(),
                store.getAddress(),
                store.getDescription(),
                List.copyOf(store.getAmenities()),
                businessHoursService.items(storeId),
                policyVersion,
                qrStatus);
    }

    /** 기존 QR을 모두 폐기하고 새 QR을 발급한다. 방문 인증용 event_store도 같은 ID로 맞춘다. */
    @Transactional
    public QrIssueResponse issueQr(Long userId, Long storeId, QrIssueRequest request) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<StoreQr> previous = qrRepository.findByStoreIdAndStatusNot(storeId, QrStatus.REVOKED);
        previous.forEach(StoreQr::revoke);
        qrRepository.flush();

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = sha256(token);
        String reason = request == null ? null : request.reason();
        StoreQr qr = qrRepository.save(StoreQr.issue(
                storeId, "https://cdn.pawfit.app/qr/" + UUID.randomUUID() + ".png", token, hash, reason));
        eventStoreSynchronizer.syncIssued(store, hash);
        return new QrIssueResponse(qr.getId(), qr.getStatus(), !previous.isEmpty());
    }

    /** 방문 인증(event 도메인)이 쓰는 해시와 같은 SHA-256 hex. */
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
