package com.advancedswiftly.pawfit.global.ai;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

abstract class AbstractHttpAiClient implements AiClient {

    AiClientException translate(String operation, RestClientException exception) {
        boolean retryable = true;
        if (exception instanceof RestClientResponseException responseException) {
            HttpStatusCode status = responseException.getStatusCode();
            retryable = status.value() == 429 || status.is5xxServerError();
        }
        return new AiClientException(operation + "에 실패했습니다.", exception, retryable);
    }

    String vaccinationPrompt() {
        return "마스킹된 반려견 예방접종 증명서에서 정보를 추출하세요. "
                + "추측하지 말고 보이지 않는 값은 null로 두세요. 다음 JSON만 반환하세요: "
                + "{\"dogName\":string|null,\"clinicName\":string|null,"
                + "\"vaccinations\":[{\"name\":string,\"administeredDate\":\"YYYY-MM-DD\"|null}],"
                + "\"confidence\":0.0}";
    }
}
