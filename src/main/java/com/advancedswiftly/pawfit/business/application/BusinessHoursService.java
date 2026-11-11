package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursItem;
import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursResponse;
import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursUpdateItem;
import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursUpdateRequest;
import com.advancedswiftly.pawfit.business.api.dto.BusinessHoursUpdateResponse;
import com.advancedswiftly.pawfit.business.domain.StoreBusinessHours;
import com.advancedswiftly.pawfit.business.infrastructure.StoreBusinessHoursRepository;
import com.advancedswiftly.pawfit.business.infrastructure.StoreRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessHoursService {

    private final StoreBusinessHoursRepository hoursRepository;
    private final StoreRepository storeRepository;
    private final ManagedStoreChecker managedStoreChecker;

    public BusinessHoursService(
            StoreBusinessHoursRepository hoursRepository,
            StoreRepository storeRepository,
            ManagedStoreChecker managedStoreChecker) {
        this.hoursRepository = hoursRepository;
        this.storeRepository = storeRepository;
        this.managedStoreChecker = managedStoreChecker;
    }

    @Transactional(readOnly = true)
    public BusinessHoursResponse get(Long userId, Long storeId) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        return new BusinessHoursResponse(items(storeId));
    }

    /** 요일 오름차순(월→일)으로 영업시간 항목을 만든다. */
    List<BusinessHoursItem> items(Long storeId) {
        return hoursRepository.findByStoreId(storeId).stream()
                .sorted(Comparator.comparing(StoreBusinessHours::getDayOfWeek))
                .map(BusinessHoursItem::from)
                .toList();
    }

    @Transactional
    public BusinessHoursUpdateResponse update(Long userId, Long storeId, BusinessHoursUpdateRequest request) {
        managedStoreChecker.requireManagedStore(userId, storeId);
        if (!storeRepository.existsById(storeId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        validate(request.hours());

        Map<DayOfWeek, StoreBusinessHours> existing = hoursRepository.findByStoreId(storeId).stream()
                .collect(Collectors.toMap(StoreBusinessHours::getDayOfWeek, Function.identity()));
        for (BusinessHoursUpdateItem item : request.hours()) {
            boolean closed = Boolean.TRUE.equals(item.closed());
            StoreBusinessHours current = existing.get(item.dayOfWeek());
            if (current == null) {
                hoursRepository.save(StoreBusinessHours.of(
                        storeId, item.dayOfWeek(), item.openTime(), item.closeTime(), closed));
            } else {
                current.change(item.openTime(), item.closeTime(), closed);
            }
        }
        return new BusinessHoursUpdateResponse(storeId, request.hours().size());
    }

    private void validate(List<BusinessHoursUpdateItem> hours) {
        Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (BusinessHoursUpdateItem item : hours) {
            if (!seen.add(item.dayOfWeek())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 요일이 중복되었습니다.");
            }
            if (!Boolean.TRUE.equals(item.closed())) {
                if (item.openTime() == null || item.closeTime() == null) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "영업하는 요일은 시작·종료 시각이 필요합니다.");
                }
                if (!item.openTime().isBefore(item.closeTime())) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");
                }
            }
        }
    }
}
