package com.advancedswiftly.pawfit.business.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NtsBusinessStatusClientTest {

    private MockRestServiceServer server;
    private NtsBusinessStatusClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NtsBusinessStatusClient("test-key+with/symbols=", builder.build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"01", "02", "03", ""})
    void onlyContinuingBusinessIsActive(String statusCode) {
        server.expect(requestTo("https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=test-key%2Bwith%2Fsymbols%3D&returnType=JSON"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().json("{\"b_no\":[\"1234567890\"]}"))
                .andRespond(withSuccess("""
                        {"status_code":"OK","request_cnt":1,"data":[{"b_no":"1234567890","b_stt_cd":"%s"}]}
                        """.formatted(statusCode), MediaType.APPLICATION_JSON));
        assertThat(client.isActive("1234567890")).isEqualTo(statusCode.equals("01"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status_code\":\"ERROR\"}",
            "{\"status_code\":\"OK\",\"request_cnt\":1,\"data\":[]}",
            "{\"status_code\":\"OK\",\"request_cnt\":1,\"data\":[{\"b_no\":\"0000000000\",\"b_stt_cd\":\"01\"}]}",
            "{\"status_code\":\"OK\",\"request_cnt\":1,\"data\":[{\"b_no\":\"1234567890\",\"b_stt_cd\":\"99\"}]}",
            "{\"status_code\":\"OK\",\"request_cnt\":1,\"data\":[{\"b_no\":\"1234567890\",\"b_stt_cd\":{}}]}",
            "{\"status_code\":\"OK\",\"request_cnt\":1,\"data\":[{\"b_no\":\"1234567890\"}]}", "not-json"})
    void malformedOrMismatchedResponseIsUnavailable(String body) {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.odcloud.kr/")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertUnavailable(client);
    }

    @Test
    void missingKeyDoesNotCallExternalApi() {
        assertUnavailable(new NtsBusinessStatusClient("", RestClient.create()));
        server.verify();
    }

    @Test
    void httpErrorIsUnavailable() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.odcloud.kr/")))
                .andRespond(withServerError());
        assertUnavailable(client);
    }

    @Test
    void timeoutIsUnavailable() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.odcloud.kr/")))
                .andRespond(withException(new SocketTimeoutException("timeout")));
        assertUnavailable(client);
    }

    private void assertUnavailable(NtsBusinessStatusClient target) {
        assertThatThrownBy(() -> target.isActive("1234567890"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_STATUS_UNAVAILABLE))
                .hasMessageNotContaining("test-key").hasMessageNotContaining("serviceKey");
    }
}
