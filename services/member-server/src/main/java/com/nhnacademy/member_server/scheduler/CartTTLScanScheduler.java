package com.nhnacademy.member_server.scheduler;

import com.nhnacademy.member_server.service.CartService;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


// 트래픽이 엄청 몰렸을 때 SCAN 방식에서 ZSET 방식으로 바꾸면 됨 확장성!

@Service
@RequiredArgsConstructor
@Slf4j
public class CartTTLScanScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CartService cartService;

    private static final long TTL_THRESHOLD_SECONDS = 30 * 60L; // 30분
    private static final int SCAN_COUNT = 300;

    @SchedulerLock(name = "CartRedisToDBLock", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    @Scheduled(fixedDelay = 60_000) // 1분
    public void syncExpiringCarts() {

        // scan -> non-blocking 장애 x
        ScanOptions options = ScanOptions.scanOptions()
                .match("cart:m:*")
                .count(SCAN_COUNT)
                .build();


        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) return;

        try (RedisConnection connection = factory.getConnection()) {
            if (connection == null) return;

            try (Cursor<byte[]> cursor = connection.scan(options)) {

                while (cursor.hasNext()) {
                    String cartKey = new String(cursor.next());

                    Long ttl = redisTemplate.getExpire(cartKey, TimeUnit.SECONDS);
                    if (ttl == null || ttl < 0) {
                        continue; // TTL 없음 or 이미 만료
                    }

                    if (ttl > TTL_THRESHOLD_SECONDS) {
                        continue; // 아직 멀었음
                    }

                    Long memberId = extractMemberId(cartKey);
                    if (memberId == null) continue;

                    Map<Object, Object> redisItems =
                            redisTemplate.opsForHash().entries(cartKey);

                    // 이유 내가 짠 코드는 장바구니가 비면 무조건 CART_STATUS 키가 남아있기 떄문에
                    if (redisItems.isEmpty()) {
                        continue;
                    }

                    try {
                        // 세션 종료 시점 단 1회 DB 반영
                        cartService.syncToDb(memberId, redisItems);
                    } catch (Exception e) {
                        log.error("DB sync failed for memberId={}", memberId, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cart TTL scan scheduler failed", e);
        }
    }

    private Long extractMemberId(String cartKey) {
        try {
            return Long.parseLong(cartKey.substring("cart:m:".length()));
        } catch (Exception e) {
            log.warn("잘못된 Redis Key 형식이 감지됨: {}", cartKey);
            return null;
        }
    }
}
