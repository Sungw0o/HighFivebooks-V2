package com.nhnacademy.coupon_server.client;

import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false", // 테스트 중 유레카 등록 방지
        "spring.batch.job.enabled=false",
        // FeignClient가 실제 유레카 대신 로컬 WireMock 포트를 바라보게 설정
        "spring.cloud.openfeign.client.config.TEAM5-MEMBER-SERVER.url=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0) // 랜덤 포트로 가짜 서버(WireMock) 실행
@ActiveProfiles("test")
class MemberServiceClientTest {

    @Autowired
    private MemberServiceClient memberServiceClient;


    @Test
    @DisplayName("멤버 서버 생일자 조회 요청 테스트")
    void getBirthdayUserId_Success(){
        int month = 12;
        int page = 0;
        int size = 10;
        List<Long> expectedMemberIds = List.of(10L, 20L, 30L);

        stubFor(get(urlPathEqualTo("/api/members/birthday"))
                .withQueryParam("month", equalTo(String.valueOf(month)))
                .withQueryParam("page", equalTo(String.valueOf(page)))
                .withQueryParam("size", equalTo(String.valueOf(size)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[10, 20, 30]")));

        List<Long> result = memberServiceClient.getBirthdayUserId(month, page, size);

        assertThat(result).isEqualTo(expectedMemberIds);

        verify(getRequestedFor(urlPathEqualTo("/api/members/birthday"))
                .withQueryParam("month", equalTo(String.valueOf(month)))
                .withQueryParam("page", equalTo("0"))
                .withQueryParam("size", equalTo("10")));
    }
}