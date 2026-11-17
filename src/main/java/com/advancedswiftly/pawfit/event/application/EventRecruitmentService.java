package com.advancedswiftly.pawfit.event.application;

import com.advancedswiftly.pawfit.event.domain.StampEvent;
import com.advancedswiftly.pawfit.event.infrastructure.StampEventRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/** 다른 도메인에 이벤트 모집 판단에 필요한 값만 제공한다. */
@Service
public class EventRecruitmentService {

    private final StampEventRepository events;

    public EventRecruitmentService(StampEventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Recruitment get(Long eventId, LocalDate baseDate) {
        StampEvent event =
                events.findDetailById(eventId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        return new Recruitment(event.getRegionIds(), event.isRecruitingAt(baseDate));
    }

    public record Recruitment(Set<Long> regionIds, boolean open) {}
}
