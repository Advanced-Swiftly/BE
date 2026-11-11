package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreCreateRequest;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreCreateResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreDetailResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreListItem;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreListResponse;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreUpdateRequest;
import com.advancedswiftly.pawfit.admin.api.dto.AdminStoreUpdateResponse;
import com.advancedswiftly.pawfit.admin.api.dto.StoreRecheckRequest;
import com.advancedswiftly.pawfit.admin.api.dto.StoreRecheckResponse;
import com.advancedswiftly.pawfit.admin.api.dto.StoreVisibilityRequest;
import com.advancedswiftly.pawfit.admin.api.dto.StoreVisibilityResponse;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.business.domain.ConditionRequest;
import com.advancedswiftly.pawfit.business.domain.Store;
import com.advancedswiftly.pawfit.business.domain.StoreManager;
import com.advancedswiftly.pawfit.business.domain.StorePolicy;
import com.advancedswiftly.pawfit.business.domain.StoreQr;
import com.advancedswiftly.pawfit.business.domain.StoreVisibility;
import com.advancedswiftly.pawfit.business.infrastructure.ConditionRequestRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreManagerRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StorePolicyRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreQrRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 운영자의 업체 목록·상세·등록·수정·노출 변경과 정책 재검토 요청. */
@Service
public class AdminStoreService {

    public static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;
    private final StoreRepository storeRepository;
    private final StoreManagerRepository managerRepository;
    private final StorePolicyRepository policyRepository;
    private final StoreQrRepository qrRepository;
    private final ConditionRequestRepository requestRepository;
    private final UserRepository userRepository;

    public AdminStoreService(
            JdbcTemplate jdbcTemplate,
            StoreRepository storeRepository,
            StoreManagerRepository managerRepository,
            StorePolicyRepository policyRepository,
            StoreQrRepository qrRepository,
            ConditionRequestRepository requestRepository,
            UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.storeRepository = storeRepository;
        this.managerRepository = managerRepository;
        this.policyRepository = policyRepository;
        this.qrRepository = qrRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminStoreListResponse list(Long regionId, StoreVisibility visibility, String keyword, int page) {
        StringBuilder sql = new StringBuilder(
                "SELECT store_id, name, visibility, region_id, verification_status FROM store WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (regionId != null) {
            sql.append(" AND region_id = ?");
            args.add(regionId);
        }
        if (visibility != null) {
            sql.append(" AND visibility = ?");
            args.add(visibility.name());
        }
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND name LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY store_id DESC LIMIT ? OFFSET ?");
        args.add(PAGE_SIZE);
        args.add((page - 1) * PAGE_SIZE);
        List<AdminStoreListItem> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AdminStoreListItem(
                        rs.getLong(1), rs.getString(2), rs.getString(3), rs.getObject(4, Long.class), rs.getString(5)),
                args.toArray());
        return new AdminStoreListResponse(items);
    }

    @Transactional(readOnly = true)
    public AdminStoreDetailResponse get(Long storeId) {
        Store store = find(storeId);
        Long managerId = managerRepository
                .findById(storeId)
                .map(StoreManager::getUserId)
                .orElseGet(() -> approvedSubmitter(storeId));
        AdminStoreDetailResponse.Manager manager = managerId == null
                ? null
                : new AdminStoreDetailResponse.Manager(
                        managerId, userRepository.findById(managerId).map(User::getName).orElse(null));
        Integer policyVersion = policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(StorePolicy::getVersion)
                .orElse(null);
        Long pending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM condition_request WHERE store_id = ? AND status IN ('RECEIVED', 'IN_REVIEW')",
                Long.class,
                storeId);
        String qrStatus = qrRepository
                .findFirstByStoreIdOrderByIdDesc(storeId)
                .map(StoreQr::getStatus)
                .map(Enum::name)
                .orElse(null);
        return new AdminStoreDetailResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                store.getDescription(),
                store.getRegionId(),
                store.getVisibility().name(),
                store.getVerificationStatus().name(),
                manager,
                policyVersion,
                pending == null ? 0L : pending,
                qrStatus);
    }

    @Transactional
    public AdminStoreCreateResponse create(AdminStoreCreateRequest request) {
        requireRegion(request.regionId());
        Store store = storeRepository.save(Store.registerByOperator(
                request.name().trim(),
                request.phoneNumber(),
                request.address().trim(),
                request.latitude(),
                request.longitude(),
                request.regionId()));
        return new AdminStoreCreateResponse(store.getId(), store.getVisibility().name());
    }

    @Transactional
    public AdminStoreUpdateResponse update(Long storeId, AdminStoreUpdateRequest request) {
        Store store = find(storeId);
        if (request.managerUserId() != null) {
            User user = userRepository
                    .findById(request.managerUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 사용자입니다."));
            if (user.getRole() != UserRole.BUSINESS) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "업체 관리자 역할의 사용자만 지정할 수 있습니다.");
            }
            managerRepository.findById(storeId).ifPresentOrElse(
                    manager -> manager.changeUser(user.getId()),
                    () -> managerRepository.save(new StoreManager(storeId, user.getId())));
        }
        store.updateByOperator(
                request.name() == null ? null : request.name().trim(),
                request.address() == null ? null : request.address().trim(),
                request.description());
        return new AdminStoreUpdateResponse(store.getId(), store.getName());
    }

    @Transactional
    public StoreVisibilityResponse changeVisibility(Long storeId, StoreVisibilityRequest request) {
        Store store = find(storeId);
        store.changeVisibility(request.visible() ? StoreVisibility.VISIBLE : StoreVisibility.HIDDEN);
        return new StoreVisibilityResponse(store.getId(), store.getVisibility().name());
    }

    /** 현재 정책 버전을 기준으로 재검토 요청을 만든다. 정책이 없으면 기준 버전은 0. */
    @Transactional
    public StoreRecheckResponse recheck(Long operatorId, Long storeId, StoreRecheckRequest request) {
        find(storeId);
        int baseVersion = policyRepository
                .findFirstByStoreIdOrderByVersionDesc(storeId)
                .map(StorePolicy::getVersion)
                .orElse(0);
        ConditionRequest saved = requestRepository.save(
                ConditionRequest.recheck(storeId, baseVersion, request.reason().trim(), operatorId));
        return new StoreRecheckResponse(saved.getId(), saved.getKind().name(), saved.getStatus());
    }

    private Store find(Long storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void requireRegion(Long regionId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM region WHERE region_id = ?", Long.class, regionId);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 지역입니다.");
        }
    }

    /** 업체 관리자가 따로 지정되지 않은 업체는 승인된 인증을 요청한 사용자를 관리자로 본다. */
    private Long approvedSubmitter(Long storeId) {
        List<Long> submitters = jdbcTemplate.query(
                "SELECT submitted_by FROM store_verification WHERE store_id = ? AND status = 'APPROVED' "
                        + "ORDER BY verification_id DESC LIMIT 1",
                (rs, rowNum) -> rs.getLong(1),
                storeId);
        return submitters.isEmpty() ? null : submitters.get(0);
    }
}
