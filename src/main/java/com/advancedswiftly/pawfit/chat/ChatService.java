package com.advancedswiftly.pawfit.chat;

import com.advancedswiftly.pawfit.global.ai.AiChatMessage;
import com.advancedswiftly.pawfit.global.ai.AiChatRequest;
import com.advancedswiftly.pawfit.global.ai.AiService;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.place.PlaceService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
class ChatService {
    private static final int HISTORY_SIZE = 20;
    private final JdbcTemplate db;
    private final AiService ai;
    private final PlaceService places;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    ChatService(JdbcTemplate db, AiService ai, PlaceService places, ObjectMapper json,
            PlatformTransactionManager manager) {
        this.db = db;
        this.ai = ai;
        this.places = places;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
    }

    public Sessions sessions(long userId, int page, int size) {
        return new Sessions(db.query("""
                SELECT session_id, title, last_message_at FROM chat_session WHERE user_id=?
                ORDER BY last_message_at DESC NULLS LAST, session_id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> new Session(rs.getLong(1), rs.getString(2),
                rs.getObject(3, OffsetDateTime.class)), userId, size, (long) page * size));
    }

    public Created create(long userId, String title) {
        String displayTitle = title == null || title.isBlank() ? "새 대화" : title.strip();
        long id = insert("INSERT INTO chat_session(user_id,title) VALUES (?,?)", "session_id", userId, displayTitle);
        return new Created(id, displayTitle);
    }

    public void delete(long userId, long sessionId) {
        if (db.update("DELETE FROM chat_session WHERE session_id=? AND user_id=?", sessionId, userId) == 0) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
    }

    public History history(long userId, long sessionId, String cursor) {
        return transaction.execute(ignored -> {
            requireSession(userId, sessionId);
            long before = parseCursor(cursor);
            List<Message> rows = db.query("""
                    SELECT message_id, role, content, places_json FROM chat_message
                    WHERE session_id=? AND message_id < ? ORDER BY message_id DESC LIMIT ?
                    """, (rs, index) -> new Message(rs.getLong(1), rs.getString(2), rs.getString(3),
                    readPlaces(rs.getString(4))), sessionId, before, HISTORY_SIZE + 1);
            List<Message> messages = rows.stream().limit(HISTORY_SIZE).toList();
            String next = rows.size() > HISTORY_SIZE ? Long.toString(messages.getLast().messageId()) : null;
            return new History(messages.reversed(), next);
        });
    }

    public Sent send(Authentication auth, long userId, long sessionId, ChatController.Send request) {
        long revision = requireSession(userId, sessionId);
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var candidates = places.chatCandidates(auth, request.selectedDogIds(), request.latitude(), request.longitude());
        var previous = db.query("""
                SELECT role, content FROM chat_message WHERE session_id=?
                ORDER BY message_id DESC LIMIT 8
                """, (rs, index) -> new AiChatMessage(rs.getString(1).toLowerCase(Locale.ROOT), rs.getString(2)),
                sessionId);
        var context = new ArrayList<>(previous.reversed());
        context.add(new AiChatMessage("user", request.content().strip()));
        String prompt = """
                PawFit 반려견 동반 장소 안내를 한국어로 답하세요.
                아래 후보에 있는 장소만 추천하고, 후보의 판정을 그대로 따르세요.
                CHECK_REQUIRED는 조건 확인이 필요하다는 뜻입니다. 입장을 보장하지 마세요.
                장소 추천이 필요하지 않으면 placeIds를 빈 배열로 응답하세요.
                JSON 객체 {"content":"답변","placeIds":["STORE:1"]} 형식만 출력하세요.
                후보 데이터:
                """ + json.writeValueAsString(candidates.stream()
                .map(card -> java.util.Map.of("placeId", card.placeId(), "name", card.name(),
                        "verdict", card.verdict())).toList());

        // 외부 응답을 기다리는 동안 DB 트랜잭션이나 세션 잠금을 점유하지 않습니다.
        Answer answer = answer(new AiChatRequest(prompt, context), candidates);
        return transaction.execute(ignored -> {
            if (db.update("""
                    UPDATE chat_session SET revision=revision+1,last_message_at=?
                    WHERE session_id=? AND user_id=? AND revision=?
                    """, OffsetDateTime.now(ZoneOffset.UTC), sessionId, userId, revision) == 0) {
                requireSession(userId, sessionId);
                throw new BusinessException(ErrorCode.CHAT_SESSION_CHANGED);
            }
            long userMessageId = saveMessage(sessionId, "USER", request.content().strip(), List.of());
            long assistantId = saveMessage(sessionId, "ASSISTANT", answer.content(), answer.places());
            return new Sent(userMessageId, new Assistant(assistantId, answer.content(), answer.places()));
        });
    }

    private Answer answer(AiChatRequest request, List<PlaceService.Card> candidates) {
        try {
            String raw = ai.chat(request).content();
            if (raw == null) throw unavailable();
            String payload = raw.strip();
            if (payload.startsWith("```") && payload.endsWith("```") && payload.indexOf('\n') > 0) {
                payload = payload.substring(payload.indexOf('\n') + 1, payload.length() - 3).strip();
            }
            JsonNode result = json.readTree(payload);
            String content = result.path("content").asText("").strip();
            if (content.isBlank() || content.length() > 4000 || !result.path("placeIds").isArray()) {
                throw unavailable();
            }
            var allowed = new LinkedHashMap<String, PlaceRef>();
            for (var candidate : candidates) {
                allowed.put(candidate.placeId(), new PlaceRef(candidate.placeId(), candidate.verdict().name()));
            }
            var selected = new ArrayList<PlaceRef>();
            for (JsonNode id : result.path("placeIds")) {
                PlaceRef place = allowed.remove(id.asText(""));
                if (place != null) selected.add(place);
                if (selected.size() == 5) break;
            }
            return new Answer(content, List.copyOf(selected));
        } catch (JacksonException exception) {
            throw unavailable();
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.AI_UNAVAILABLE) throw unavailable();
            throw exception;
        }
    }

    private long requireSession(long userId, long sessionId) {
        return db.query("SELECT revision FROM chat_session WHERE session_id=? AND user_id=?",
                (rs, index) -> rs.getLong(1), sessionId, userId).stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    private long parseCursor(String cursor) {
        if (cursor == null) return Long.MAX_VALUE;
        try {
            long value = Long.parseLong(cursor);
            if (value > 0) return value;
        } catch (NumberFormatException ignored) {
            // 아래 공통 오류 형식으로 처리합니다.
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private long saveMessage(long sessionId, String role, String content, List<PlaceRef> references) {
        return insert("INSERT INTO chat_message(session_id,role,content,places_json) VALUES (?,?,?,?)",
                "message_id", sessionId, role, content, json.writeValueAsString(references));
    }

    private long insert(String sql, String column, Object... arguments) {
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] {column});
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            return statement;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    private List<PlaceRef> readPlaces(String payload) {
        return List.of(json.readValue(payload, PlaceRef[].class));
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.CHAT_SERVICE_UNAVAILABLE);
    }

    record Session(long sessionId, String title, OffsetDateTime lastMessageAt) {}
    record Sessions(List<Session> items) {}
    record Created(long sessionId, String title) {}
    record PlaceRef(String placeId, String verdict) {}
    record Message(long messageId, String role, String content, List<PlaceRef> places) {}
    record History(List<Message> messages, String nextCursor) {}
    record Assistant(long messageId, String content, List<PlaceRef> places) {}
    record Sent(long userMessageId, Assistant assistantMessage) {}
    private record Answer(String content, List<PlaceRef> places) {}
}
