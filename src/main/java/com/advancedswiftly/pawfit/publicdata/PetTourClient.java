package com.advancedswiftly.pawfit.publicdata;

import com.advancedswiftly.pawfit.global.http.ExternalRestClientFactory;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Component
class PetTourClient {
    private static final String ENDPOINT =
            "https://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2";
    private static final Map<String, String> CATEGORIES = Map.of(
            "12", "ATTRACTION", "14", "CULTURE", "15", "EVENT", "28", "LEISURE",
            "32", "LODGING", "38", "SHOPPING", "39", "FOOD");

    private final String key;
    private final org.springframework.web.client.RestClient client =
            ExternalRestClientFactory.create(Duration.ofSeconds(15));

    PetTourClient(@Value("${pawfit.places.data-go-kr-service-key:}") String key) {
        this.key = key;
    }

    boolean configured() {
        return !key.isBlank();
    }

    Batch collect(Instant since) {
        Map<String, Row> rows = new LinkedHashMap<>();
        int seen = 0;
        int failed = 0;
        for (int page = 1; page <= 200; page++) {
            JsonNode response = client.get().uri(uri(page, since)).retrieve().body(JsonNode.class);
            if (response == null) throw new IllegalStateException("관광공사 응답 없음");
            JsonNode root = response.path("response");
            String code = root.path("header").path("resultCode").asText();
            if (!code.equals("0000") && !code.equals("00")) {
                throw new IllegalStateException("관광공사 API 오류");
            }
            JsonNode body = root.path("body");
            int total = body.path("totalCount").asInt(-1);
            JsonNode item = body.path("items").path("item");
            var entries = item.isArray() ? item.values() : item.isObject() ? List.of(item) : List.<JsonNode>of();
            int count = 0;
            for (JsonNode entry : entries) {
                Row row = normalize(entry);
                if (row == null) failed++;
                else rows.put(row.id(), row);
                count++;
            }
            if (total < 0 || (count == 0 && seen < total)) {
                throw new IllegalStateException("관광공사 페이지 응답 오류");
            }
            seen += count;
            if (seen >= total) return new Batch(List.copyOf(rows.values()), failed);
        }
        throw new IllegalStateException("관광공사 페이지 한도 초과");
    }

    private URI uri(int page, Instant since) {
        var uri = UriComponentsBuilder.fromUriString(ENDPOINT).queryParam("serviceKey", "{key}")
                .queryParam("MobileOS", "ETC").queryParam("MobileApp", "PawFit")
                .queryParam("_type", "json").queryParam("arrange", "A")
                .queryParam("pageNo", page).queryParam("numOfRows", 100);
        if (since != null) {
            uri.queryParam("modifiedtime", LocalDate.ofInstant(since, ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        return uri.encode().buildAndExpand(key).toUri();
    }

    static Row normalize(JsonNode item) {
        String id = item.path("contentid").asText("").strip();
        String name = item.path("title").asText("").strip();
        if (!id.matches("[0-9]{1,80}") || name.isBlank()) return null;
        try {
            BigDecimal latitude = new BigDecimal(item.path("mapy").asText("").strip());
            BigDecimal longitude = new BigDecimal(item.path("mapx").asText("").strip());
            if (latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                    || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0) return null;
            String address = (item.path("addr1").asText("") + " " + item.path("addr2").asText("")).strip();
            return new Row(id, limit(name, 150), address.isEmpty() ? null : limit(address, 255),
                    CATEGORIES.get(item.path("contenttypeid").asText("")),
                    latitude.setScale(7, java.math.RoundingMode.HALF_UP),
                    longitude.setScale(7, java.math.RoundingMode.HALF_UP),
                    regionId(item.path("lDongRegnCd").asText("")));
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private static String limit(String value, int length) {
        return value.substring(0, value.offsetByCodePoints(0,
                Math.min(length, value.codePointCount(0, value.length()))));
    }

    private static Long regionId(String code) {
        return code.matches("[0-9]{2}") ? Long.valueOf(code) : null;
    }

    record Row(String id, String name, String address, String category,
               BigDecimal latitude, BigDecimal longitude, Long regionId) {}
    record Batch(List<Row> rows, int failed) {}
}
