package com.nhnacademy.member_server.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nhnacademy.member_server.service.CartService;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

@ExtendWith(MockitoExtension.class)
class CartTTLScanSchedulerTest {

    @InjectMocks
    private CartTTLScanScheduler scheduler;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CartService cartService;

    // Redis 내부 동작 Mocking을 위한 객체들
    @Mock private RedisConnectionFactory connectionFactory;
    @Mock private RedisConnection connection;
    @Mock private Cursor<byte[]> cursor;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    void setUp() {
        // RedisTemplate이 HashOperations를 반환하도록 설정
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    // ==========================================
    // 1. Connection 관련 예외/Edge Case 테스트
    // ==========================================

    @Test
    @DisplayName("RedisConnectionFactory가 null이면 바로 종료")
    void syncExpiringCarts_FactoryNull_Returns() {
        given(redisTemplate.getConnectionFactory()).willReturn(null);

        scheduler.syncExpiringCarts();

        verify(redisTemplate, never()).opsForHash();
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("RedisConnection을 가져올 수 없으면(null) 바로 종료")
    void syncExpiringCarts_ConnectionNull_Returns() {
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(null);

        scheduler.syncExpiringCarts();

        verify(redisTemplate, never()).opsForHash();
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("Scan 실행 중 예외 발생 시 로그 남기고 종료 (전체 try-catch)")
    void syncExpiringCarts_ScanException_Logged() {
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);

        // ScanOptions 매처 처리 주의
        given(connection.scan(any(ScanOptions.class))).willThrow(new RuntimeException("Redis connection failed"));

        scheduler.syncExpiringCarts();

        // 예외가 발생해도 앱이 죽지 않았는지 확인
        verifyNoInteractions(cartService);
    }

    // ==========================================
    // 2. Cursor Loop 및 로직 테스트 (Happy Path + Mixed Cases)
    // ==========================================

    @Test
    @DisplayName("복합 시나리오: 정상 대상, TTL 김, 포맷 오류, 만료된 키 혼재")
    void syncExpiringCarts_MixedScenarios() {
        // Given
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);
        given(connection.scan(any(ScanOptions.class))).willReturn(cursor);

        // 시나리오용 키 데이터 준비
        String validKey = "cart:m:1";       // 정상 (TTL 1000초)
        String highTtlKey = "cart:m:2";     // 무시 (TTL 5000초 > 30분)
        String expiredKey = "cart:m:3";     // 무시 (TTL null or -1)
        String invalidFormatKey = "cart:m:X"; // 무시 (파싱 불가)

        // 1. Cursor Mocking (순서대로 키를 반환하도록 설정)
        // hasNext(): T, T, T, T, F (4개 조회 후 종료)
        when(cursor.hasNext()).thenReturn(true, true, true, true, false);
        // next(): 각 키를 바이트 배열로 반환
        when(cursor.next()).thenReturn(
                validKey.getBytes(StandardCharsets.UTF_8),
                highTtlKey.getBytes(StandardCharsets.UTF_8),
                expiredKey.getBytes(StandardCharsets.UTF_8),
                invalidFormatKey.getBytes(StandardCharsets.UTF_8)
        );

        // 2. TTL Mocking
        // TimeUnit.SECONDS 주의
        given(redisTemplate.getExpire(validKey, TimeUnit.SECONDS)).willReturn(1000L); // 대상 O
        given(redisTemplate.getExpire(highTtlKey, TimeUnit.SECONDS)).willReturn(5000L); // 대상 X
        given(redisTemplate.getExpire(expiredKey, TimeUnit.SECONDS)).willReturn(-2L); // 만료됨
        given(redisTemplate.getExpire(invalidFormatKey, TimeUnit.SECONDS)).willReturn(1000L); // 대상 O (하지만 파싱 실패 예정)

        // 3. Redis 데이터 Mocking
        Map<Object, Object> items = new HashMap<>();
        items.put("bookId", "1");
        given(hashOperations.entries(validKey)).willReturn(items);

        // When
        scheduler.syncExpiringCarts();

        // Then
        // 1. Valid Key -> syncToDb 호출됨
        verify(cartService).syncToDb(1L, items);

        // 2. High TTL Key -> 호출 안 됨
        verify(cartService, never()).syncToDb(eq(2L), any());

        // 3. Expired Key -> 호출 안 됨
        verify(cartService, never()).syncToDb(eq(3L), any());

        // 4. Invalid Format Key -> 호출 안 됨 (파싱에서 걸러짐)
        // extractMemberId 내부의 catch 블록을 타게 됨
    }

    // ==========================================
    // 3. Service 예외 처리 테스트
    // ==========================================

    @Test
    @DisplayName("syncToDb 실행 중 예외가 발생해도 Loop는 계속 돌아야 함")
    void syncExpiringCarts_ServiceException_ContinuesLoop() {
        // Given
        String errorKey = "cart:m:100";
        String nextKey = "cart:m:101";

        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);
        given(connection.scan(any(ScanOptions.class))).willReturn(cursor);

        // Cursor 설정: 에러나는 키 -> 정상 키 -> 종료
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(
                errorKey.getBytes(StandardCharsets.UTF_8),
                nextKey.getBytes(StandardCharsets.UTF_8)
        );

        // TTL 설정
        given(redisTemplate.opsForHash()).willReturn(hashOperations);

        // [해결책 2] 스케줄러가 키를 통해 데이터를 조회할 때 비어있지 않은 맵을 반환하도록 설정
        // anyString()을 써서 모든 키 조회에 대해 더미 데이터를 반환하게 함
        given(hashOperations.entries(anyString())).willReturn(Map.of("1", "1"));

        // TTL 설정
        given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(60L);

        // Service 예외 설정
        // [중요] any() 매처를 사용할 때 Map 타입이 맞는지 확인 (anyMap() 사용 권장)
        willThrow(new RuntimeException("DB Connection Error"))
                .given(cartService).syncToDb(eq(100L), anyMap());

        // When
        scheduler.syncExpiringCarts();

        // Then
        verify(cartService).syncToDb(eq(100L), anyMap());
        verify(cartService).syncToDb(eq(101L), anyMap());
    }

    // ==========================================
    // 4. Edge Case: TTL Null
    // ==========================================
    @Test
    @DisplayName("TTL이 null인 경우(영구 키 등) 스킵")
    void syncExpiringCarts_TTLNull_Skip() {
        String key = "cart:m:1";
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);
        given(connection.scan(any(ScanOptions.class))).willReturn(cursor);

        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key.getBytes(StandardCharsets.UTF_8));

        // TTL null 반환
        given(redisTemplate.getExpire(key, TimeUnit.SECONDS)).willReturn(null);

        scheduler.syncExpiringCarts();

        verify(cartService, never()).syncToDb(anyLong(), any());
    }
}