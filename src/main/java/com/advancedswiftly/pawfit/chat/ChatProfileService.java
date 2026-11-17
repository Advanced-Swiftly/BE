package com.advancedswiftly.pawfit.chat;

import com.advancedswiftly.pawfit.dog.infrastructure.DogRepository;
import com.advancedswiftly.pawfit.global.error.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChatProfileService {
    private final DogRepository dogs;
    private final ChatProfileActionRepository actions;

    public ChatProfileService(DogRepository dogs, ChatProfileActionRepository actions) {
        this.dogs = dogs;
        this.actions = actions;
    }

    /**
     * Only a direct, named add-equipment command produces a proposal; no model output is executed.
     */
    @Transactional
    public Proposal propose(long userId, long sessionId, String content) {
        var match =
                java.util.regex.Pattern.compile(
                                "^(.{1,50}?)\\s*프로필에?\\s+(유모차|이동장|매너벨트|리드줄|입마개)(?:를|을)?\\s*(?:추가|등록)(?:해\\s*줘|해줘|해|해주세요)?[.!]?$ "
                                        .strip())
                        .matcher(content.strip());
        if (!match.matches()) return null;
        var named =
                dogs.findAllByUserIdOrderByDefaultDogDescIdAsc(userId).stream()
                        .filter(d -> d.getName().equals(match.group(1).strip()))
                        .toList();
        if (named.size() != 1)
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "반려견 이름을 정확하게 지정해 주세요.");
        String code =
                switch (match.group(2)) {
                    case "유모차" -> "STROLLER";
                    case "이동장" -> "CARRIER";
                    case "매너벨트" -> "BELLY_BAND";
                    case "리드줄" -> "LEASH";
                    default -> "MUZZLE";
                };
        var dog = named.get(0);
        var action = actions.save(new ChatProfileAction(userId, sessionId, dog.getId(), code));
        return new Proposal(action.getId(), dog.getId(), dog.getName(), code, "추가하기");
    }

    @Transactional
    public Map<String, Object> confirm(long userId, long sessionId, UUID id) {
        var action =
                actions.lock(id)
                        .filter(a -> a.matches(userId, sessionId))
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (action.isApplied()) return Map.of("applied", true, "dogId", action.getDogId());
        if (action.isExpired())
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_STATE, "변경 확인 시간이 만료됐습니다. 다시 요청해 주세요.");
        var dog =
                dogs.findByIdAndUserId(action.getDogId(), userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_OWNED));
        var codes = new ArrayList<>(dog.getEquipmentCodes());
        if (!codes.contains(action.getEquipmentCode())) codes.add(action.getEquipmentCode());
        dog.replaceEquipmentCodes(codes);
        action.apply();
        return Map.of("applied", true, "dogId", dog.getId(), "equipmentCodes", codes);
    }

    public record Proposal(
            UUID actionId, Long dogId, String dogName, String equipmentCode, String confirmLabel) {}
}
