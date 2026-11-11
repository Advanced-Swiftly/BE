package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.business.domain.Store;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 업체 QR을 발급·변경할 때 event_store에 같은 ID(store_id = store.id)의 행을 맞춘다.
 * 방문 인증·스탬프·통계가 event_store.store_id를 기준으로 하므로, 두 테이블의 ID를 같게 유지하는 것이 전제다.
 * 지역(region_id)이 없거나 진행 중(OPEN)인 이벤트가 없으면 조용히 건너뛴다.
 */
@Component
public class EventStoreSynchronizer {

    private final JdbcTemplate jdbcTemplate;

    public EventStoreSynchronizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 새 QR의 해시로 event_store 행을 만들거나 갱신한다. */
    public void syncIssued(Store store, String qrTokenHash) {
        Long storeId = store.getId();
        int updated = jdbcTemplate.update(
                "UPDATE event_store SET name = ?, qr_token_hash = ?, qr_revoked = FALSE WHERE store_id = ?",
                store.getName(), qrTokenHash, storeId);
        if (updated > 0 || store.getRegionId() == null) {
            return;
        }
        List<Object[]> events = jdbcTemplate.query(
                "SELECT event_id, ends_on FROM stamp_event WHERE status = 'OPEN' ORDER BY event_id DESC",
                (rs, rowNum) -> new Object[] {rs.getLong(1), rs.getDate(2)});
        if (events.isEmpty()) {
            return;
        }
        Long eventId = (Long) events.get(0)[0];
        LocalDate endsOn = ((Date) events.get(0)[1]).toLocalDate();
        jdbcTemplate.update(
                "INSERT INTO event_store (store_id, event_id, region_id, name, latitude, longitude, approved, "
                        + "qr_token_hash, qr_expires_at, qr_revoked) VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, ?, FALSE)",
                storeId, eventId, store.getRegionId(), store.getName(), store.getLatitude(), store.getLongitude(),
                qrTokenHash, java.sql.Timestamp.from(endsOn.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
    }

    /** QR을 끄거나 다시 켤 때 event_store의 qr_revoked를 맞춘다. */
    public void syncRevoked(Long storeId, boolean revoked) {
        jdbcTemplate.update("UPDATE event_store SET qr_revoked = ? WHERE store_id = ?", revoked, storeId);
    }
}
