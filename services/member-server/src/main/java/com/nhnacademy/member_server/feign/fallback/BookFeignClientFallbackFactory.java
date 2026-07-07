package com.nhnacademy.member_server.feign.fallback;

import com.nhnacademy.member_server.dto.response.cart.GetBookResponse;
import com.nhnacademy.member_server.feign.BookFeignClient;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookFeignClientFallbackFactory implements FallbackFactory<BookFeignClient> {

    private static final int MAX_FALLBACK_ITEMS = 1000;

    @Override
    public BookFeignClient create(Throwable cause) {
        return bookIds -> {
            log.error("📚 Book Service 연결 실패 (Fallback 실행). 원인: {}", Objects.toString(cause.getMessage(), "Unknown error"));

            if (bookIds == null || bookIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 대량 데이터 방어
            int size = Math.min(bookIds.size(), MAX_FALLBACK_ITEMS);
            // 상품 정보는 없지만 ID와 수량은 보여주기 위해 더미 객체 반환
            return bookIds.stream()
                    .limit(size)
                    .map(id -> new GetBookResponse(id, "상품 정보를 불러올 수 없습니다.", 0, null))
                    .toList();
        };
    }
}