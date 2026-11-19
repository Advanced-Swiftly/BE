package com.advancedswiftly.pawfit.chat;

import com.advancedswiftly.pawfit.place.PlaceService;

import java.util.List;
import java.util.Locale;

/** Reuse phrasing, never cached eligibility: every answer receives fresh database facts. */
final class ChatPolicyAnswer {
    private ChatPolicyAnswer() {}

    static boolean supports(String question) {
        String text = question.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (List.of(
                        "만약", "하면", "라면", "없으면", "없이", "빼고", "변경", "추가", "등록", "수정", "비교", "추천",
                        "영업", "전화", "주소", "사진", "운영", "예약", "주차", "메뉴", "가격", "이벤트", "스탬프", "qr",
                        "후기", "리뷰", "날씨", "몇시", "실내", "야외", "실외")
                .stream()
                .anyMatch(text::contains)) return false;
        return List.of("입장", "동반", "데려", "갈수", "가도", "준비", "조건", "필요", "가능", "체중", "무게", "kg")
                .stream()
                .anyMatch(text::contains);
    }

    static String render(PlaceService.ChatPlace place) {
        String dogs = place.dogNames().isEmpty() ? "반려견" : String.join("·", place.dogNames());
        String result =
                switch (place.card().verdict()) {
                    case ENTRY_OK -> dogs + " 기준으로 입장 가능해요.";
                    case PREPARE -> dogs + "와 동반하려면 준비가 필요해요.";
                    case ENTRY_DENIED -> dogs + " 기준으로 현재 입장 조건에 맞지 않아요.";
                    case CHECK_REQUIRED -> "입장 가능 여부를 판단하려면 추가 확인이 필요해요.";
                };
        StringBuilder text = new StringBuilder(place.card().name()).append(": ").append(result);
        if (place.dogNames().isEmpty()) text.append(" 기준 반려견을 선택하거나 등록된 이름을 함께 알려 주세요.");
        if (place.area() != null)
            text.append(" ").append(place.area().equals("INDOOR") ? "실내" : "야외").append(" 기준이에요.");
        if (place.maxWeightKg() != null)
            text.append(" 체중은 ")
                    .append(place.maxWeightKg().stripTrailingZeros().toPlainString())
                    .append(place.maxWeightExclusive() ? "kg 미만" : "kg 이하")
                    .append(" 조건이에요.");
        if (!place.requiredEquipmentCodes().isEmpty())
            text.append(" 필수 준비물: ")
                    .append(
                            String.join(
                                    ", ",
                                    place.requiredEquipmentCodes().stream()
                                            .map(ChatPolicyAnswer::equipment)
                                            .toList()))
                    .append(".");
        var missing =
                place.reasons().stream()
                        .filter(
                                r ->
                                        r.endsWith("_REQUIRED")
                                                && !r.endsWith("_VACCINATION_REQUIRED"))
                        .map(r -> equipment(r.substring(0, r.length() - "_REQUIRED".length())))
                        .toList();
        if (!missing.isEmpty())
            text.append(" 현재 프로필에 없는 준비물: ").append(String.join(", ", missing)).append(".");
        if (place.reasons().contains("WEIGHT_LIMIT_EXCEEDED")) text.append(" 등록된 체중이 장소 제한을 넘어요.");
        if (place.reasons().contains("WEIGHT_UNKNOWN")) text.append(" 반려견 체중 등록이 필요해요.");
        if (place.reasons().contains("BREED_RESTRICTED")) text.append(" 견종 제한에 해당해요.");
        if (place.reasons().stream().anyMatch(r -> r.endsWith("_VACCINATION_REQUIRED")))
            text.append(" 필수 예방접종 확인이 필요해요.");
        if (!place.detailsComplete()) text.append(" 세부 입장 조건이 모두 확인되지 않아 방문 전 업체에 확인해 주세요.");
        if (place.confirmedOn() != null)
            text.append(" 정책 확인일: ").append(place.confirmedOn()).append(".");
        return text.toString();
    }

    private static String equipment(String code) {
        return switch (code) {
            case "STROLLER" -> "유모차";
            case "CARRIER" -> "이동장";
            case "MUZZLE" -> "입마개";
            case "BELLY_BAND" -> "매너벨트";
            case "LEASH" -> "리드줄";
            default -> code;
        };
    }
}
