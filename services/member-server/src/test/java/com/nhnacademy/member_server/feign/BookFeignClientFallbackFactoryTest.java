package com.nhnacademy.member_server.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.member_server.dto.response.cart.GetBookResponse;
import com.nhnacademy.member_server.feign.fallback.BookFeignClientFallbackFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookFeignClientFallbackFactoryTest {

    private final BookFeignClientFallbackFactory fallbackFactory = new BookFeignClientFallbackFactory();

    @Test
    @DisplayName("Fallback Factory가 생성한 클라이언트가 더미 데이터를 반환하는지 테스트")
    void createFallbackClient() {
        // given
        Throwable cause = new RuntimeException("Connection Refused");

        // when
        BookFeignClient fallbackClient = fallbackFactory.create(cause);
        List<GetBookResponse> responses = fallbackClient.getBooksBulk(List.of(1L, 2L));

        // then
        assertThat(responses).hasSize(2);

        // 첫 번째 더미 데이터 검증
        GetBookResponse response1 = responses.getFirst();
        assertThat(response1.bookId()).isEqualTo(1L);
        assertThat(response1.title()).isEqualTo("상품 정보를 불러올 수 없습니다.");
        assertThat(response1.price()).isZero();
        assertThat(response1.image()).isNull();
    }

    @Test
    @DisplayName("Fallback Client에 빈 리스트 요청 시 빈 리스트 반환")
    void fallbackClient_EmptyList() {
        BookFeignClient fallbackClient = fallbackFactory.create(new Exception());
        List<GetBookResponse> responses = fallbackClient.getBooksBulk(List.of());

        assertThat(responses).isEmpty();
    }
}