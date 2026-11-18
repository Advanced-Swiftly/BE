package com.advancedswiftly.pawfit.publicdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

class PetTourClientTest {
    @ParameterizedTest
    @CsvSource({"FD050100,CAFE", "FD050200,CAFE", "FD020300,FOOD", "UNKNOWN,FOOD"})
    void officialCafeAndTeahouseCodesRemainInCafeFilterAfterSync(String code, String category) {
        var item = JsonMapper.builder().build().readTree("""
                {"contentid":"2938669","title":"원본 장소","mapy":"37.5446","mapx":"127.0557",
                 "contenttypeid":"39","lDongRegnCd":"11","lclsSystm3":"%s"}
                """.formatted(code));
        var normalized = PetTourClient.normalize(item);
        assertThat(normalized).isNotNull();
        assertThat(normalized.category()).isEqualTo(category);
        assertThat(normalized.name()).isEqualTo("원본 장소");
        assertThat(normalized.regionId()).isEqualTo(11L);
    }
}
