package com.advancedswiftly.pawfit.event.application;

import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;
import com.advancedswiftly.pawfit.event.api.dto.EventDtos.*;
import com.advancedswiftly.pawfit.event.domain.*;
import com.advancedswiftly.pawfit.event.infrastructure.*;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private static final int PAGE_SIZE = 20;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Instant MAX_QUERY_TIME = Instant.parse("9999-12-31T23:59:59Z");
    private final StampEventRepository eventRepository;
    private final EventStoreRepository storeRepository;
    private final VisitRepository visitRepository;
    private final EventStampRepository stampRepository;
    private final DrawEntryRepository drawEntryRepository;
    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final Clock clock = Clock.systemUTC();

    public EventService(StampEventRepository eventRepository, EventStoreRepository storeRepository,
                        VisitRepository visitRepository, EventStampRepository stampRepository,
                        DrawEntryRepository drawEntryRepository, UserRepository userRepository,
                        DogRepository dogRepository) {
        this.eventRepository = eventRepository; this.storeRepository = storeRepository;
        this.visitRepository = visitRepository; this.stampRepository = stampRepository;
        this.drawEntryRepository = drawEntryRepository; this.userRepository = userRepository;
        this.dogRepository = dogRepository;
    }

    @Transactional(readOnly = true)
    public CurrentEventResponse findCurrent(Long userId, Long regionId) {
        requireGuardian(userId, false);
        LocalDate today = today();
        StampEvent event = eventRepository
                .findFirstByStatusAndStartsOnLessThanEqualAndEndsOnGreaterThanEqualOrderByStartsOnDesc(
                        EventStatus.OPEN, today, today)
                .filter(found -> regionId == null || found.getRegionIds().contains(regionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        return new CurrentEventResponse(event.getId(), event.getTitle(), event.getYear(),
                event.getQuarter(), event.getStatus());
    }

    @Transactional(readOnly = true)
    public EventDetailResponse findDetail(Long userId, Long eventId) {
        requireGuardian(userId, false);
        StampEvent event = requireEvent(eventId);
        return new EventDetailResponse(event.getId(), event.getTitle(), event.getStartsOn(), event.getEndsOn(),
                event.getRegionIds().stream().sorted().toList(),
                event.getPrizes().stream().map(prize ->
                        new PrizeResponse(prize.getRank(), prize.getName(), prize.getQuantity())).toList(),
                event.getNotice());
    }

    @Transactional(readOnly = true)
    public EventStoreListResponse findStores(Long userId, Long eventId, Long regionId,
                                              BigDecimal latitude, BigDecimal longitude) {
        requireGuardian(userId, false);
        requireEvent(eventId);
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<EventStoreItem> items = (regionId == null
                        ? storeRepository.findAllByEventIdAndApprovedTrueOrderById(eventId)
                        : storeRepository.findAllByEventIdAndRegionIdAndApprovedTrueOrderById(eventId, regionId))
                .stream()
                .map(store -> new EventStoreItem(store.getId(), store.getName(),
                        distanceMeters(latitude, longitude, store)))
                .sorted(Comparator.comparing(EventStoreItem::distanceMeters,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(EventStoreItem::storeId))
                .toList();
        return new EventStoreListResponse(items);
    }

    @Transactional
    public QrVisitResponse authenticateVisit(Long userId, QrVisitRequest request) {
        User user = requireGuardian(userId, true);
        List<Long> dogIds = request.dogIds().stream().distinct().toList();
        if (dogIds.size() != request.dogIds().size()
                || dogRepository.countByIdInAndUserId(dogIds, userId) != dogIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String hash = QrTokenHash.sha256(request.qrToken());
        EventStore store = storeRepository.findByQrTokenHash(hash)
                .filter(found -> found.isQrUsableAt(clock.instant()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OR_REVOKED_QR));
        if (visitRepository.existsByUserIdAndQrTokenHash(userId, hash)) {
            throw new BusinessException(ErrorCode.INVALID_OR_REVOKED_QR);
        }

        Visit visit = visitRepository.save(Visit.create(user, store, hash, clock.instant(), dogIds));
        StampEvent event = store.getEvent();
        boolean awarded = event.isOpenAt(today())
                && !stampRepository.existsByEventIdAndUserIdAndStoreId(event.getId(), userId, store.getId())
                && stampRepository.countByEventIdAndUserId(event.getId(), userId) < event.getMaxStampCount();
        if (awarded) {
            stampRepository.save(EventStamp.create(event, user, store, clock.instant()));
        }
        long stampCount = stampRepository.countByEventIdAndUserId(event.getId(), userId);
        return new QrVisitResponse(visit.getId(), store.getId(), awarded, stampCount);
    }

    @Transactional(readOnly = true)
    public VisitListResponse findVisits(Long userId, LocalDate from, LocalDate to, int page) {
        requireGuardian(userId, false);
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Instant fromInstant = from == null ? Instant.EPOCH : from.atStartOfDay(SEOUL).toInstant();
        Instant toInstant = to == null ? MAX_QUERY_TIME : to.plusDays(1).atStartOfDay(SEOUL).toInstant().minusNanos(1);
        List<VisitItem> items = visitRepository.findAllByUserIdAndVisitedAtBetween(
                        userId, fromInstant, toInstant,
                        PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "visitedAt")))
                .stream().map(visit -> new VisitItem(
                        visit.getId(), visit.getStore().getName(), visit.getVisitedAt())).toList();
        return new VisitListResponse(items);
    }

    @Transactional(readOnly = true)
    public StampStatusResponse findStamps(Long userId, Long eventId) {
        requireGuardian(userId, false);
        StampEvent event = requireEvent(eventId);
        Set<Long> awardedStoreIds = stampRepository.findAllByEventIdAndUserId(eventId, userId)
                .stream().map(stamp -> stamp.getStore().getId()).collect(java.util.stream.Collectors.toSet());
        List<StampStoreItem> stores = storeRepository.findAllByEventIdAndApprovedTrueOrderById(eventId)
                .stream().map(store -> new StampStoreItem(store.getId(), awardedStoreIds.contains(store.getId())))
                .toList();
        return new StampStatusResponse(eventId, awardedStoreIds.size(), event.getMaxStampCount(), stores);
    }

    @Transactional
    public DrawEntryCreateResponse enterDraw(Long userId, Long eventId, DrawEntryRequest request) {
        User user = requireGuardian(userId, true);
        StampEvent event = requireEvent(eventId);
        if (!event.isOpenAt(today()) || !Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        long stampCount = stampRepository.countByEventIdAndUserId(eventId, userId);
        if (stampCount < event.getRequiredStampCount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STAMPS);
        }
        if (drawEntryRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new BusinessException(ErrorCode.DRAW_ENTRY_ALREADY_EXISTS);
        }
        DrawEntry entry = drawEntryRepository.save(DrawEntry.create(
                event, user, request.phoneNumber(), request.address().trim(), clock.instant()));
        return new DrawEntryCreateResponse(entry.getId(), entry.getStatus(), stampCount);
    }

    @Transactional(readOnly = true)
    public DrawEntryResponse findDrawEntry(Long userId, Long eventId) {
        requireGuardian(userId, false);
        requireEvent(eventId);
        DrawEntry entry = drawEntryRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAW_ENTRY_NOT_FOUND));
        return new DrawEntryResponse(entry.getId(), entry.getStatus(),
                entry.getPrize() == null ? null : new PrizeNameResponse(entry.getPrize().getName()),
                entry.getFulfillmentStatus());
    }

    private StampEvent requireEvent(Long eventId) {
        return eventRepository.findDetailById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
    }

    private User requireGuardian(Long userId, boolean lock) {
        User user = (lock ? userRepository.findByIdForUpdate(userId) : userRepository.findById(userId))
                .filter(User::isActive).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getRole() != UserRole.GUARDIAN) throw new BusinessException(ErrorCode.FORBIDDEN);
        return user;
    }

    private LocalDate today() { return LocalDate.now(clock.withZone(SEOUL)); }

    private Long distanceMeters(BigDecimal lat, BigDecimal lon, EventStore store) {
        if (lat == null || store.getLatitude() == null || store.getLongitude() == null) return null;
        double lat1 = Math.toRadians(lat.doubleValue());
        double lat2 = Math.toRadians(store.getLatitude().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(store.getLongitude().doubleValue() - lon.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return Math.round(6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
