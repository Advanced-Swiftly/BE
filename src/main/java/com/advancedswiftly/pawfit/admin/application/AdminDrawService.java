package com.advancedswiftly.pawfit.admin.application;

import com.advancedswiftly.pawfit.admin.api.dto.EventDrawRequest;
import com.advancedswiftly.pawfit.admin.api.dto.EventDrawResponse;
import com.advancedswiftly.pawfit.admin.domain.EventDraw;
import com.advancedswiftly.pawfit.admin.infrastructure.EventDrawRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 이벤트 추첨 실행. 이벤트 테이블은 이벤트 도메인 소유라 SQL로 다룬다. */
@Service
public class AdminDrawService {

    private final JdbcTemplate jdbcTemplate;
    private final EventDrawRepository drawRepository;

    public AdminDrawService(JdbcTemplate jdbcTemplate, EventDrawRepository drawRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.drawRepository = drawRepository;
    }

    /**
     * 종료(CLOSED)된 이벤트에서 한 번만 실행한다.
     * 응모(ENTERED) 중 필요 스탬프 수를 채운 응모만 후보가 되고, 시드로 섞은 순서대로 순위가 높은 경품부터
     * 수량만큼 배정한다. 나머지 응모는 NOT_SELECTED가 된다. 이벤트 상태는 바꾸지 않는다(DRAWN은 별도 변경).
     */
    @Transactional
    public EventDrawResponse draw(Long operatorId, Long eventId, EventDrawRequest request) {
        if (!Boolean.TRUE.equals(request.confirm())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "추첨 실행 확인이 필요합니다.");
        }
        List<Object[]> events = jdbcTemplate.query(
                "SELECT status, required_stamp_count FROM stamp_event WHERE event_id = ?",
                (rs, rowNum) -> new Object[] {rs.getString(1), rs.getInt(2)},
                eventId);
        if (events.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (drawRepository.existsById(eventId)) {
            throw new BusinessException(ErrorCode.DRAW_ALREADY_EXECUTED);
        }
        if (!"CLOSED".equals(events.get(0)[0])) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        int requiredStamps = (Integer) events.get(0)[1];

        Map<Long, Long> stampCounts = new HashMap<>();
        jdbcTemplate.query(
                "SELECT user_id, COUNT(*) FROM event_stamp WHERE event_id = ? GROUP BY user_id",
                rs -> {
                    stampCounts.put(rs.getLong(1), rs.getLong(2));
                },
                eventId);
        List<Long> eligible = new ArrayList<>();
        jdbcTemplate.query(
                "SELECT draw_entry_id, user_id FROM draw_entry WHERE event_id = ? AND status = 'ENTERED' "
                        + "ORDER BY draw_entry_id",
                rs -> {
                    if (stampCounts.getOrDefault(rs.getLong(2), 0L) >= requiredStamps) {
                        eligible.add(rs.getLong(1));
                    }
                },
                eventId);

        List<Long> prizeSlots = new ArrayList<>();
        jdbcTemplate.query(
                "SELECT prize_id, quantity FROM event_prize WHERE event_id = ? ORDER BY prize_rank",
                rs -> {
                    for (int i = 0; i < rs.getInt(2); i++) {
                        prizeSlots.add(rs.getLong(1));
                    }
                },
                eventId);

        String seed = StringUtils.hasText(request.seed()) ? request.seed().trim() : UUID.randomUUID().toString();
        Collections.shuffle(eligible, new Random(seedToLong(seed)));
        int winnerCount = Math.min(prizeSlots.size(), eligible.size());
        for (int i = 0; i < winnerCount; i++) {
            jdbcTemplate.update(
                    "UPDATE draw_entry SET status = 'WON', prize_id = ?, fulfillment_status = 'CONTACT_PENDING' "
                            + "WHERE draw_entry_id = ?",
                    prizeSlots.get(i), eligible.get(i));
        }
        jdbcTemplate.update(
                "UPDATE draw_entry SET status = 'NOT_SELECTED' WHERE event_id = ? AND status = 'ENTERED'", eventId);

        Instant drawnAt = Instant.now();
        drawRepository.save(new EventDraw(eventId, drawnAt, seed, winnerCount, operatorId));
        return new EventDrawResponse(eventId, drawnAt, winnerCount);
    }

    /** 같은 시드는 항상 같은 숫자가 되도록 SHA-256 앞 8바이트를 쓴다. */
    private static long seedToLong(String seed) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(seed.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
