package com.nhnacademy.order_server.adapter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.order_server.config.FeignResilienceConfig;
import com.nhnacademy.order_server.dto.request.CouponCalculationRequest;
import com.nhnacademy.order_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.order_server.dto.request.PointTransactionRequest;
import com.nhnacademy.order_server.dto.request.StockRequest;
import com.nhnacademy.order_server.dto.response.CouponCalculationResponse;
import com.nhnacademy.order_server.dto.response.external.BookInfoResponse;
import com.nhnacademy.order_server.dto.response.external.MemberGradeResponse;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Feign 경계(계약) 테스트.
 *
 * <p>실제 Feign 인프라(SpringMvcContract, 인코더/디코더, timeout, {@link FeignResilienceConfig}의
 * NEVER_RETRY)를 그대로 사용하고, 외부 서버는 JDK 내장 {@link HttpServer} 스텁으로 대체한다.
 * 이렇게 하면 별도 mock 라이브러리(pom) 추가 없이 요청 경로/메서드/헤더/바디 계약과
 * 응답 매핑, 실패 처리를 검증할 수 있다.
 */
@SpringBootTest(
        classes = FeignClientBoundaryTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.openfeign.circuitbreaker.enabled=false",
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.openfeign.client.config.default.connectTimeout=1000",
                "spring.cloud.openfeign.client.config.default.readTimeout=3000"
        }
)
class FeignClientBoundaryTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            RabbitAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    @EnableFeignClients(clients = {BookClient.class, MemberClient.class, CouponClient.class, PaymentClient.class})
    @Import(FeignResilienceConfig.class)
    static class TestApp {
    }

    private static HttpServer server;

    // 스텁 응답/요청 기록 (테스트가 순차 실행되므로 static 공유로 충분하다)
    private static volatile int stubStatus = 200;
    private static volatile String stubBody = "";
    private static volatile Recorded recorded;
    private static final AtomicInteger callCount = new AtomicInteger();

    @Autowired
    private BookClient bookClient;
    @Autowired
    private MemberClient memberClient;
    @Autowired
    private CouponClient couponClient;
    @Autowired
    private PaymentClient paymentClient;

    @DynamicPropertySource
    static void feignUrls(DynamicPropertyRegistry registry) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", FeignClientBoundaryTest::handle);
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        registry.add("book.service.url", () -> base);
        registry.add("member.service.url", () -> base);
        registry.add("coupon.service.url", () -> base);
        registry.add("payment.service.url", () -> base);
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void reset() {
        stubStatus = 200;
        stubBody = "";
        recorded = null;
        callCount.set(0);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        callCount.incrementAndGet();
        byte[] body = exchange.getRequestBody().readAllBytes();
        recorded = new Recorded(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders(),
                new String(body, UTF_8)
        );
        byte[] response = stubBody.getBytes(UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        if (response.length == 0) {
            exchange.sendResponseHeaders(stubStatus, -1);
        } else {
            exchange.sendResponseHeaders(stubStatus, response.length);
            exchange.getResponseBody().write(response);
        }
        exchange.close();
    }

    private record Recorded(String method, String path, String query, Headers headers, String body) {
    }

    // =====================================================================
    // BookClient
    // =====================================================================

    @Test
    @DisplayName("book: 도서 정보 조회 요청 계약과 응답 매핑을 검증한다")
    void bookGetBooksBulk() {
        stubStatus = 200;
        stubBody = "[{\"bookId\":1,\"price\":10000,\"accumulateRate\":0.05,\"title\":\"클린 코드\"}]";

        ResponseEntity<List<BookInfoResponse>> response = bookClient.getBooksBulk(List.of(1L, 2L));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getBookId()).isEqualTo(1L);
        assertThat(response.getBody().getFirst().getPrice()).isEqualTo(10000);
        assertThat(response.getBody().getFirst().getTitle()).isEqualTo("클린 코드");

        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/api/books/bulk");
        assertThat(recorded.body()).contains("1").contains("2");
    }

    @Test
    @DisplayName("book: 재고 선점 성공 시 orderKey 쿼리와 바디 계약을 지킨다")
    void bookHoldStockSuccess() {
        stubStatus = 200;
        stubBody = "";

        ResponseEntity<Void> response = bookClient.holdStockBatch(List.of(new StockRequest(1L, 2)), "ORDER-KEY-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/api/books/stock/hold/batch");
        assertThat(recorded.query()).isEqualTo("orderKey=ORDER-KEY-1");
        assertThat(recorded.body()).contains("\"bookId\":1").contains("\"quantity\":2");
    }

    @Test
    @DisplayName("book: 재고 선점 실패(500)는 FeignException을 던지고 재시도하지 않는다")
    void bookHoldStockFailureDoesNotRetry() {
        stubStatus = 500;
        stubBody = "{\"message\":\"stock error\"}";

        assertThatThrownBy(() -> bookClient.holdStockBatch(List.of(new StockRequest(1L, 2)), "ORDER-KEY-2"))
                .isInstanceOf(FeignException.class);

        // FeignResilienceConfig.NEVER_RETRY 로 인해 정확히 1회만 호출되어야 한다
        assertThat(callCount.get()).isEqualTo(1);
    }

    // =====================================================================
    // MemberClient
    // =====================================================================

    @Test
    @DisplayName("member: 회원 등급 조회 요청 경로와 적립률 응답을 검증한다")
    void memberGetGrade() {
        stubStatus = 200;
        stubBody = "{\"gradeName\":\"GOLD\",\"earnRate\":0.05}";

        MemberGradeResponse grade = memberClient.getMemberGrade(7L);

        assertThat(grade.getGradeName()).isEqualTo("GOLD");
        assertThat(grade.getEarnRate()).isEqualTo(0.05);
        assertThat(recorded.method()).isEqualTo("GET");
        assertThat(recorded.path()).isEqualTo("/api/members/7/grade");
    }

    @Test
    @DisplayName("member: 포인트 예약 성공 시 TCC reserve 경로와 바디 계약을 지킨다")
    void memberReservePointSuccess() {
        stubStatus = 200;
        stubBody = "";

        memberClient.reservePoint(PointTransactionRequest.builder()
                .memberId(7L).amount(500L).orderId(11L).build());

        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/internal/point-transactions/tcc/reserve");
        assertThat(recorded.body())
                .contains("\"memberId\":7")
                .contains("\"amount\":500")
                .contains("\"orderId\":11");
    }

    @Test
    @DisplayName("member: 포인트 예약 실패(500)는 FeignException을 던진다")
    void memberReservePointFailure() {
        stubStatus = 500;
        stubBody = "{\"message\":\"point error\"}";

        assertThatThrownBy(() -> memberClient.reservePoint(PointTransactionRequest.builder()
                .memberId(7L).amount(500L).orderId(11L).build()))
                .isInstanceOf(FeignException.class);
        assertThat(callCount.get()).isEqualTo(1);
    }

    // =====================================================================
    // CouponClient
    // =====================================================================

    @Test
    @DisplayName("coupon: 할인 계산 시 X-USER-ID 헤더와 응답 매핑을 검증한다")
    void couponCalculate() {
        stubStatus = 200;
        stubBody = "{\"discountAmount\":2000,\"finalPrice\":8000}";

        CouponCalculationResponse response =
                couponClient.calculateCoupon(7L, new CouponCalculationRequest(3L, 10000L));

        assertThat(response.getDiscountAmount()).isEqualTo(2000L);
        assertThat(response.getFinalPrice()).isEqualTo(8000L);
        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/api/coupons/calculate");
        assertThat(recorded.headers().getFirst("X-USER-ID")).isEqualTo("7");
        assertThat(recorded.body()).contains("\"couponId\":3").contains("\"totalOrderPrice\":10000");
    }

    // =====================================================================
    // PaymentClient
    // =====================================================================

    @Test
    @DisplayName("payment: 결제 취소 시 paymentKey 경로와 취소 바디 계약을 지킨다")
    void paymentCancel() {
        stubStatus = 200;
        stubBody = "";

        paymentClient.cancelPayment("PAY-KEY-9", new PaymentCancelRequest("고객 변심", 8000));

        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/api/payments/PAY-KEY-9/cancel");
        assertThat(recorded.body()).contains("고객 변심").contains("8000");
    }
}
