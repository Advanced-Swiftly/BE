package com.advancedswiftly.pawfit.business.infrastructure;

import com.advancedswiftly.pawfit.business.application.BusinessStatusClient;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import com.advancedswiftly.pawfit.global.http.ExternalRestClientFactory;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Component
public class NtsBusinessStatusClient implements BusinessStatusClient {

    private static final String ENDPOINT = "https://api.odcloud.kr/api/nts-businessman/v1/status";
    private static final Set<String> STATUS_CODES = Set.of("01", "02", "03", "");

    private final String serviceKey;
    private final RestClient client;

    @Autowired
    public NtsBusinessStatusClient(
            @Value("${pawfit.business-status.service-key:}") String serviceKey,
            @Value("${pawfit.business-status.timeout:5s}") Duration timeout) {
        this(serviceKey, ExternalRestClientFactory.create(timeout));
    }

    NtsBusinessStatusClient(String serviceKey, RestClient client) {
        this.serviceKey = serviceKey;
        this.client = client;
    }

    @Override
    public boolean isActive(String businessNumber) {
        if (serviceKey.isBlank()) {
            throw unavailable();
        }
        URI uri = UriComponentsBuilder.fromUriString(ENDPOINT)
                .queryParam("serviceKey", "{key}")
                .queryParam("returnType", "JSON")
                .encode().buildAndExpand(serviceKey).toUri();
        try {
            JsonNode response = client.post().uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("b_no", List.of(businessNumber)))
                    .retrieve().body(JsonNode.class);
            if (response == null || !"OK".equals(response.path("status_code").asText())
                    || response.path("request_cnt").asInt(-1) != 1) {
                throw unavailable();
            }
            JsonNode data = response.path("data");
            if (!data.isArray() || data.size() != 1) {
                throw unavailable();
            }
            JsonNode item = data.get(0);
            if (!businessNumber.equals(item.path("b_no").asText()) || !item.path("b_stt_cd").isTextual()) {
                throw unavailable();
            }
            String statusCode = item.path("b_stt_cd").asText();
            if (!STATUS_CODES.contains(statusCode)) {
                throw unavailable();
            }
            return "01".equals(statusCode);
        } catch (RestClientException | IllegalArgumentException exception) {
            // 외부 오류에 포함될 수 있는 serviceKey·URL·응답 본문을 클라이언트에 노출하지 않는다.
            throw unavailable();
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException(ErrorCode.BUSINESS_STATUS_UNAVAILABLE);
    }
}
