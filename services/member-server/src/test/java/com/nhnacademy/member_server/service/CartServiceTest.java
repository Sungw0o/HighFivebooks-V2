package com.nhnacademy.member_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.dto.request.cart.CartAddRequest;
import com.nhnacademy.member_server.dto.request.cart.CartItemUpdateRequest;
import com.nhnacademy.member_server.dto.response.cart.CartAddResponse;
import com.nhnacademy.member_server.dto.response.cart.CartListResponse;
import com.nhnacademy.member_server.dto.response.cart.CartUpdateResponse;
import com.nhnacademy.member_server.dto.response.cart.GetBookResponse;
import com.nhnacademy.member_server.entity.cart.Cart;
import com.nhnacademy.member_server.entity.cart.CartItem;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.feign.BookFeignClient;
import com.nhnacademy.member_server.repository.CartItemRepository;
import com.nhnacademy.member_server.repository.CartRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.impl.CartServiceImpl;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @InjectMocks
    private CartServiceImpl cartService;

    @Mock
    private RedisTemplate<String, String> luaRedisTemplate;
    @Mock
    private BookFeignClient bookFeignClient;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private DefaultRedisScript<Long> cartUpsertScript;
    @Mock
    private DefaultRedisScript<Long> cartMergeScript;

    // Redis Operations Mock
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // RedisTemplate의 opsForHash, opsForValue 호출 시 Mock 객체 반환 설정
        lenient().when(luaRedisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(luaRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cartService = new CartServiceImpl(
                luaRedisTemplate,
                bookFeignClient,
                cartItemRepository,
                cartRepository,
                memberRepository,
                cartUpsertScript,
                cartMergeScript
        );
    }

    // ==========================================
    // 1. addToCart (장바구니 담기)
    // ==========================================
    @Test
    @DisplayName("addToCart - 정상 추가")
    void addToCart_Success() {
        // given
        Long memberId = 1L;
        CartAddRequest request = new CartAddRequest(100L, 2);

        given(luaRedisTemplate.execute(eq(cartUpsertScript), anyList(), any(), any(), any(), any(), any()))
                .willReturn(5L); // 현재 수량 5로 가정

        // when
        CartAddResponse response = cartService.addToCart(request, memberId, null);

        // then
        assertThat(response.bookId()).isEqualTo(100L);
        assertThat(response.quantity()).isEqualTo(5);
        verify(luaRedisTemplate).execute(eq(cartUpsertScript), anyList(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("addToCart - 수량이 0 이하인 경우 예외")
    void addToCart_InvalidQuantity() {
        CartAddRequest request = new CartAddRequest(100L, 0);
        assertThatThrownBy(() -> cartService.addToCart(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("addToCart - Redis 실행 중 오류 발생")
    void addToCart_RedisError() {
        CartAddRequest request = new CartAddRequest(100L, 1);
        given(luaRedisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any()))
                .willThrow(new RuntimeException("Redis Error"));

        assertThatThrownBy(() -> cartService.addToCart(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REDIS_SERVER_ERROR);
    }

    // ==========================================
    // 2. updateCartItemQuantity (수량 변경)
    // ==========================================
    @Test
    @DisplayName("updateCartItemQuantity - 정상 변경")
    void updateCartItemQuantity_Success() {
        Long memberId = 1L;
        CartItemUpdateRequest request = new CartItemUpdateRequest(100L, 3);
        String key = "cart:m:1";

        given(hashOperations.hasKey(key, "100")).willReturn(true);

        given(luaRedisTemplate.execute(
                eq(cartUpsertScript),
                anyList(),
                any(), any(), any(), any(), any()
        )).willReturn(3L);

        CartUpdateResponse response = cartService.updateCartItemQuantity(memberId, null, request);

        assertThat(response.quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("updateCartItemQuantity - 존재하지 않는 아이템 수정 시 예외")
    void updateCartItemQuantity_NotFound() {
        Long memberId = 1L;
        CartItemUpdateRequest request = new CartItemUpdateRequest(999L, 3);
        String key = "cart:m:1";

        given(hashOperations.hasKey(key, "999")).willReturn(false);

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(memberId, null, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    // ==========================================
    // 3. deleteCartItem (단건 삭제)
    // ==========================================
    @Test
    @DisplayName("deleteCartItem - 삭제 후 아이템이 남아있으면 TTL 갱신")
    void deleteCartItem_RemainingItems() {
        Long memberId = 1L;
        Long bookId = 100L;
        String key = "cart:m:1";

        given(luaRedisTemplate.hasKey(key)).willReturn(true); // 아이템 남음

        cartService.deleteCartItem(memberId, null, bookId);

        verify(hashOperations).delete(key, "100");
        verify(luaRedisTemplate).expire(eq(key), anyLong(), any()); // TTL 갱신 확인
    }

    // ==========================================
    // 4. deleteAllCartItem (장바구니 비우기 - 마커 생성)
    // ==========================================
    @Test
    @DisplayName("deleteAllCartItem - 빈 상태 마커 생성 확인")
    void deleteAllCartItem_CreatesMarker() {
        Long memberId = 1L;
        String key = "cart:m:1";

        Set<Object> keys = new HashSet<>(List.of("100", "101"));
        given(hashOperations.keys(key)).willReturn(keys);

        cartService.deleteAllCartItem(memberId, null, false);

        // 1. 기존 아이템 삭제 확인
        verify(hashOperations).delete(key, keys.toArray());
        // 2. 마커 생성 확인
        verify(hashOperations).put(key, "CART_STATUS", "EMPTY");
        // 3. Empty TTL 적용 확인
        verify(luaRedisTemplate).expire(eq(key), eq(60 * 60 * 2L), any());
    }

    // ==========================================
    // 5. restoreCartOnLogin (로그인 복구)
    // ==========================================
    @Test
    @DisplayName("restoreCartOnLogin - Redis에 이미 있으면 DB 조회 안 함")
    void restoreCartOnLogin_RedisHit() {
        Long memberId = 1L;
        String key = "cart:m:1";
        given(luaRedisTemplate.hasKey(key)).willReturn(true);

        cartService.restoreCartOnLogin(memberId);

        verify(cartItemRepository, never()).findByCart_Member_Id(anyLong());
    }

    @Test
    @DisplayName("restoreCartOnLogin - Redis 없고 DB에 데이터 있으면 복구")
    void restoreCartOnLogin_RedisMiss_DBHit() {
        Long memberId = 1L;
        String key = "cart:m:1";
        CartItem item = mock(CartItem.class);
        given(item.getBookId()).willReturn(100L);
        given(item.getQuantity()).willReturn(2);

        given(luaRedisTemplate.hasKey(key)).willReturn(false);
        given(cartItemRepository.findByCart_Member_Id(memberId)).willReturn(List.of(item));

        cartService.restoreCartOnLogin(memberId);

        verify(hashOperations).putAll(eq(key), anyMap());
        verify(luaRedisTemplate).expire(eq(key), anyLong(), any());
    }

    // ==========================================
    // 6. getCartItemList (조회 로직)
    // ==========================================
    @Test
    @DisplayName("getCartItemList - Redis 데이터 조회 및 Feign 연동 계산")
    void getCartItemList_Success() {
        Long memberId = 1L;
        String key = "cart:m:1";

        // Redis Mock: 100번 책 2권
        Map<Object, Object> redisMap = new HashMap<>();
        redisMap.put("100", "2");
        given(hashOperations.entries(key)).willReturn(redisMap);

        // Feign Mock
        GetBookResponse bookResponse = new GetBookResponse(100L, "Java Book", 10000, "img");
        given(bookFeignClient.getBooksBulk(anyList())).willReturn(List.of(bookResponse));

        CartListResponse result = cartService.getCartItemList(memberId, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalCartPrice()).isEqualTo(20000);
        assertThat(result.items().getFirst().title()).isEqualTo("Java Book");
    }

    @Test
    @DisplayName("getCartItemList - Ghost Item(책 정보 없음) 삭제 로직")
    void getCartItemList_RemoveGhostItems() {
        Long memberId = 1L;
        String key = "cart:m:1";

        // Redis에는 100, 200번 책이 있음
        Map<Object, Object> redisMap = new HashMap<>();
        redisMap.put("100", "1");
        redisMap.put("200", "1");
        given(hashOperations.entries(key)).willReturn(redisMap);

        // Feign은 100번 책 정보만 반환 (200번은 삭제됨/품절됨)
        GetBookResponse bookResponse = new GetBookResponse(100L, "Java Book", 1000, "i");
        given(bookFeignClient.getBooksBulk(anyList())).willReturn(List.of(bookResponse));

        cartService.getCartItemList(memberId, null);

        // 200번 키는 Redis에서 삭제되어야 함
        verify(hashOperations).delete(key, new Object[]{200L});
    }

    // ==========================================
    // 7. syncToDb (DB 동기화 - 핵심 로직)
    // ==========================================
    @Test
    @DisplayName("syncToDb(Map) - 락 획득 실패 시 스킵")
    void syncToDb_LockFailed_Skip() {
        Long memberId = 1L;
        String lockKey = "lock:cart:sync:1";

        // 락 획득 실패 (FALSE 반환)
        given(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any())).willReturn(false);

        cartService.syncToDb(memberId, new HashMap<>());

        // DB 로직 실행 안 됨
        verify(memberRepository, never()).findById(anyLong());
        // 락 해제도 안 함 (내가 건 게 아니니까)
        verify(luaRedisTemplate, never()).delete(lockKey);
    }

    @Test
    @DisplayName("syncToDb(Map) - 정상 동기화 (Insert/Update/Delete 혼합) 및 마커 무시")
    void syncToDb_Success_WithComplexLogic() {
        Long memberId = 1L;
        String lockKey = "lock:cart:sync:1";

        // 1. 락 획득 성공
        given(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any())).willReturn(true);

        // 2. Member 존재
        Member member = mock(Member.class);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // 3. Cart 가져오기
        Cart cart = mock(Cart.class);
        given(cartRepository.findByMember_Id(memberId)).willReturn(Optional.of(cart));

        // 4. Redis 데이터 준비 (Key: 100, 101, CART_STATUS, invalid_key)
        Map<Object, Object> redisItems = new HashMap<>();
        redisItems.put("100", "2"); // Update 대상 (DB는 1개)
        redisItems.put("101", "1"); // Insert 대상 (DB 없음)
        redisItems.put("CART_STATUS", "EMPTY"); // 무시되어야 함
        redisItems.put("invalid", "value"); // 파싱 에러 (로그 찍고 무시)

        // 5. DB 데이터 준비 (100번 책 1개, 102번 책 1개 -> 102번은 Delete 대상)
        CartItem dbItem1 = new CartItem(100L, 1, cart); // to Update
        CartItem dbItem2 = new CartItem(102L, 1, cart); // to Delete
        given(cartItemRepository.findByCart_Member_Id(memberId)).willReturn(List.of(dbItem1, dbItem2));

        // When
        cartService.syncToDb(memberId, redisItems);

        assertThat(dbItem1.getQuantity()).isEqualTo(2);

        verify(cartItemRepository).deleteAllInBatch(argThat(iterable -> {
            List<CartItem> list = (List<CartItem>) iterable;

            return list.size() == 1 && list.getFirst().getBookId().equals(102L);
        }));

        verify(cartItemRepository).saveAll(argThat(iterable -> {
            List<CartItem> list = (List<CartItem>) iterable;
            return list.size() == 1 && list.getFirst().getBookId().equals(101L);
        }));

        // E. 락 해제 확인
        verify(luaRedisTemplate).delete(lockKey);
    }

    @Test
    @DisplayName("syncToDb(Overload) - 로그아웃 시 호출")
    void syncToDb_Logout_Overload() {
        Long memberId = 1L;
        String key = "cart:m:1";
        String lockKey = "lock:cart:sync:1";

        // 1. [필수] hasKey 메서드가 true를 반환하도록 설정 (이게 없어서 실패한 것!)
        given(luaRedisTemplate.hasKey(key)).willReturn(true);

        // 2. HashOperations 연결 및 데이터 설정
        given(luaRedisTemplate.opsForHash()).willReturn(hashOperations);
        Map<Object, Object> redisItems = Map.of("100", "1");
        given(hashOperations.entries(key)).willReturn(redisItems);

        // 3. 락 설정 (setIfAbsent)
        given(luaRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any())).willReturn(true);

        // 4. Repository Mock
        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(cartRepository.findByMember_Id(memberId)).willReturn(Optional.of(mock(Cart.class)));

        // When
        cartService.syncToDb(memberId);

        // Then
        verify(cartItemRepository).findByCart_Member_Id(memberId);
        verify(luaRedisTemplate).delete(lockKey);
    }
    @Test
    @DisplayName("syncToDb - 예외 발생 시 락 해제 및 throw")
    void syncToDb_Exception_ReleasesLock() {
        Long memberId = 1L;
        String lockKey = "lock:cart:sync:1";

        given(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any())).willReturn(true);
        given(memberRepository.findById(memberId)).willThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> cartService.syncToDb(memberId, new HashMap<>()))
                .isInstanceOf(RuntimeException.class);

        // 예외가 터져도 락은 해제되어야 함 (finally 블록)
        verify(luaRedisTemplate).delete(lockKey);
    }
    @Test
    @DisplayName("migrateGuestCart - 입력값이 유효하지 않으면 실행되지 않음")
    void migrateGuestCart_InvalidInput() {
        // memberId가 null
        cartService.migrateGuestCart("guest123", null);
        // guestId가 null
        cartService.migrateGuestCart(null, 1L);
        // guestId가 빈 문자열
        cartService.migrateGuestCart("", 1L);

        // 아무런 Redis 동작도 일어나지 않아야 함
        verify(luaRedisTemplate, never()).execute(any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("migrateGuestCart - 게스트 장바구니가 없으면 병합하지 않음")
    void migrateGuestCart_NoGuestCart() {
        String guestId = "guest123";
        Long memberId = 1L;
        String guestKey = "cart:g:" + guestId;

        given(luaRedisTemplate.hasKey(guestKey)).willReturn(false); // 게스트 키 없음

        cartService.migrateGuestCart(guestId, memberId);

        verify(luaRedisTemplate, never()).execute(any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("migrateGuestCart - Lua Script 결과가 null이면 예외 발생")
    void migrateGuestCart_ScriptError() {
        String guestId = "guest123";
        Long memberId = 1L;
        String guestKey = "cart:g:" + guestId;

        given(luaRedisTemplate.hasKey(guestKey)).willReturn(true);
        // Script 실행 결과가 null 반환하도록 설정
        given(luaRedisTemplate.execute(eq(cartMergeScript), anyList(), any(), any()))
                .willReturn(null);

        assertThatThrownBy(() -> cartService.migrateGuestCart(guestId, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REDIS_SERVER_ERROR);
    }
    @Test
    @DisplayName("deleteGuestCartOnly - guestId 없으면 무시, Redis 에러 시 예외")
    void deleteGuestCartOnly_Tests() {
        // 1. Null Input
        cartService.deleteGuestCartOnly(null);
        verify(luaRedisTemplate, never()).delete(anyString());

        // 2. Redis Error
        given(luaRedisTemplate.delete(anyString())).willThrow(new RuntimeException("Redis Fail"));
        assertThatThrownBy(() -> cartService.deleteGuestCartOnly("guest123"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REDIS_SERVER_ERROR);
    }

    @Test
    @DisplayName("deleteAllCartItemForOrder - 예외 발생 시 로그만 찍고 넘어감")
    void deleteAllCartItemForOrder_ExceptionSwallowed() {
        Long memberId = 1L;
        // Redis delete 시 에러 발생 가정
        given(luaRedisTemplate.delete(anyString())).willThrow(new RuntimeException("Redis Fail"));

        // 예외가 던져지지 않아야 성공
        cartService.deleteAllCartItemForOrder(memberId);

        verify(luaRedisTemplate).delete(anyString());
    }
    @Test
    @DisplayName("getCartItemList - 모두 null이면 빈 리스트 반환")
    void getCartItemList_AllNull() {
        CartListResponse response = cartService.getCartItemList(null, null);
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("getCartItemList - Redis 데이터 파싱 에러 처리 (NumberFormatException)")
    void getCartItemList_ParsingError() {
        Long memberId = 1L;
        String key = "cart:m:1";

        Map<Object, Object> redisMap = new HashMap<>();
        redisMap.put("validId", "invalidQty"); // 수량이 숫자가 아님
        redisMap.put("notANumber", "1");       // 책 ID가 숫자가 아님

        given(hashOperations.entries(key)).willReturn(redisMap);

        CartListResponse response = cartService.getCartItemList(memberId, null);

        // 파싱 에러난 항목들은 무시되고 빈 리스트 반환되어야 함
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("getCartItemList - 비회원 장바구니 존재 여부 확인 (hasGuestCart)")
    void getCartItemList_HasGuestCart() {
        Long memberId = 1L;
        String guestId = "guest123";
        String key = "cart:m:1";

        // 회원 장바구니 내용
        Map<Object, Object> redisMap = Map.of("100", "1");
        given(hashOperations.entries(key)).willReturn(redisMap);

        // 비회원 장바구니 키 존재 여부 설정
        given(luaRedisTemplate.hasKey("cart:g:" + guestId)).willReturn(true);

        // Book Mock
        GetBookResponse bookResponse = new GetBookResponse(100L, "Book", 1000, "img");
        given(bookFeignClient.getBooksBulk(anyList())).willReturn(List.of(bookResponse));

        CartListResponse response = cartService.getCartItemList(memberId, guestId);

        assertThat(response.hasGuestCart()).isTrue();
    }
    @Test
    @DisplayName("syncToDb - 회원이 존재하지 않으면 로그 찍고 종료")
    void syncToDb_MemberNotFound() {
        Long memberId = 999L;
        String lockKey = "lock:cart:sync:999";

        given(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any())).willReturn(true);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        cartService.syncToDb(memberId, new HashMap<>());

        // 이후 로직 실행 안됨
        verify(cartRepository, never()).findByMember_Id(anyLong());
        // 락은 해제되어야 함
        verify(luaRedisTemplate).delete(lockKey);
    }

    @Test
    @DisplayName("syncToDb - Redis Map 데이터 파싱 중 에러 발생 시 해당 항목 스킵")
    void syncToDb_MapParsingError() {
        Long memberId = 1L;
        String lockKey = "lock:cart:sync:1";

        given(valueOperations.setIfAbsent(eq(lockKey), anyString(), anyLong(), any())).willReturn(true);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(mock(Member.class)));
        given(cartRepository.findByMember_Id(memberId)).willReturn(Optional.of(mock(Cart.class)));

        // Redis 데이터: 정상 1개, 에러 유발 2개
        Map<Object, Object> redisItems = new HashMap<>();
        redisItems.put("100", "5");          // 정상
        redisItems.put("invalidId", "5");    // ID 파싱 에러
        redisItems.put("101", "invalidQty"); // 수량 파싱 에러

        cartService.syncToDb(memberId, redisItems);

        // 정상인 100번만 저장되어야 함
        verify(cartItemRepository).saveAll(argThat(list -> {
            List<CartItem> items = (List<CartItem>) list;
            return items.size() == 1 && items.getFirst().getBookId() == 100L;
        }));
    }
    @Test
    @DisplayName("addToCart - Lua Script 결과가 -1이면(수량 초과 등) 예외 발생")
    void addToCart_LuaReturnMinusOne() {
        CartAddRequest request = new CartAddRequest(100L, 1);
        given(luaRedisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any()))
                .willReturn(-1L);

        assertThatThrownBy(() -> cartService.addToCart(request, 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("updateCartItemQuantity - 수량 범위 초과 시 예외")
    void updateCartItemQuantity_InvalidQty() {
        CartItemUpdateRequest request = new CartItemUpdateRequest(100L, 101); // MAX 100 초과

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(1L, null, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_QUANTITY);
    }
    @Test
    @DisplayName("restoreCartOnLogin - 내부 로직 에러 발생 시 예외를 던지지 않음")
    void restoreCartOnLogin_ExceptionSwallow() {
        Long memberId = 1L;
        // Redis 조회 시 에러 발생
        given(luaRedisTemplate.hasKey(anyString())).willThrow(new RuntimeException("Error"));

        // 예외가 발생하지 않아야 함
        cartService.restoreCartOnLogin(memberId);
    }
}