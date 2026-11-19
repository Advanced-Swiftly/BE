package com.advancedswiftly.pawfit.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.advancedswiftly.pawfit.place.PlaceService;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class ChatPolicyAnswerTest {
    private PlaceService.ChatPlace facts(
            PlaceService.Verdict verdict, List<String> reasons, boolean complete) {
        return new PlaceService.ChatPlace(
                new PlaceService.Card("STORE:2", "테라스 오브 성수", null, null, verdict),
                List.of("몽이"),
                reasons,
                new BigDecimal("10"),
                false,
                List.of("STROLLER"),
                "INDOOR",
                complete,
                LocalDate.of(2026, 11, 19),
                null);
    }

    @Test
    void answersOrdinaryAdmissionAndEquipmentQuestions() {
        assertThat(ChatPolicyAnswer.supports("테라스 오브 성수에 몽이 데려가도 돼?")).isTrue();
        assertThat(ChatPolicyAnswer.supports("24시카페 준비물 뭐 필요해?")).isTrue();
        assertThat(ChatPolicyAnswer.render(facts(PlaceService.Verdict.ENTRY_OK, List.of(), true)))
                .contains("몽이 기준으로 입장 가능", "실내", "10kg 이하", "유모차");
    }

    @Test
    void explainsMissingEquipmentAndWeightDenial() {
        assertThat(
                        ChatPolicyAnswer.render(
                                facts(
                                        PlaceService.Verdict.PREPARE,
                                        List.of("STROLLER_REQUIRED"),
                                        true)))
                .contains("준비가 필요", "현재 프로필에 없는 준비물: 유모차");
        assertThat(
                        ChatPolicyAnswer.render(
                                facts(
                                        PlaceService.Verdict.ENTRY_DENIED,
                                        List.of("WEIGHT_LIMIT_EXCEEDED"),
                                        true)))
                .contains("조건에 맞지", "체중이 장소 제한을 넘어요")
                .doesNotContain("입장 가능해요");
    }

    @Test
    void incompletePolicyNeverPromisesAdmission() {
        assertThat(
                        ChatPolicyAnswer.render(
                                facts(
                                        PlaceService.Verdict.CHECK_REQUIRED,
                                        List.of("POLICY_DETAILS_UNCONFIRMED"),
                                        false)))
                .contains("추가 확인", "방문 전 업체에 확인")
                .doesNotContain("입장 가능해요");
    }

    @Test
    void hypotheticalAndOtherIntentsUseAi() {
        for (String q :
                List.of("유모차 없이 입장 가능?", "만약 체중이 바뀌면?", "가도 되는 카페 추천", "입장 가능하면 영업시간도 알려줘", "안녕"))
            assertThat(ChatPolicyAnswer.supports(q)).as(q).isFalse();
    }
}
