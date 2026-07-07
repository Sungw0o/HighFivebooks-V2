package com.nhnacademy.member_server.service.impl;

import com.nhnacademy.member_server.dto.request.cart.CartAddRequest;
import com.nhnacademy.member_server.dto.request.cart.CartItemUpdateRequest;
import com.nhnacademy.member_server.dto.response.cart.CartAddResponse;
import com.nhnacademy.member_server.dto.response.cart.CartDetailResponse;
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
import com.nhnacademy.member_server.service.CartService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final RedisTemplate<String, String> luaRedisTemplate;
    private final BookFeignClient bookFeignClient;

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;

    private final DefaultRedisScript<Long> cartUpsertScript;
    private final DefaultRedisScript<Long> cartMergeScript;

    private static final int MAX_CART_QUANTITY = 100;

    private static final long CART_TTL_SECONDS = 60 * 60 * 12L;
    private static final long CART_EMPTY_TTL_SECONDS = 60 * 60 * 2L;

    private static String memberKey(Long memberId) { return "cart:m:" + memberId; }
    private static String guestKey(String guestId) { return "cart:g:" + guestId; }

    // redis 키 부여 메서드 (회원, 비회원 구분)
    private String getRedisKey(Long memberId, String guestId) {
        boolean hasMember = memberId != null;
        boolean hasGuest = guestId != null && !guestId.isBlank();

        if (!hasMember && !hasGuest) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (hasMember) return memberKey(memberId);
        return guestKey(guestId);
    }

    // 락 키 생성 헬퍼
    private String getSyncLockKey(Long memberId) {
        return "lock:cart:sync:" + memberId;
    }

    // TTL 초기화 메서드
    private void touchTtl(String key) {
        try {
            Boolean ok = luaRedisTemplate.expire(key, CART_TTL_SECONDS, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(ok)) {
                log.warn("TTL touch failed. key={}", key);
            }
        } catch (Exception e) {
            log.warn("TTL touch error. key={}", key, e);
        }
    }

    // 빈 카트 TTL 적용
    private void touchEmptyTtl(String key) {
        try {
            Boolean ok = luaRedisTemplate.expire(key, CART_EMPTY_TTL_SECONDS, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(ok)) {
                log.warn("Empty TTL touch failed. key={}", key);
            }
        } catch (Exception e) {
            log.warn("Empty TTL touch error. key={}", key, e);
        }
    }

    // Redis에 해당 키 존재하는지 확인 메서드
    private boolean hasKey(String key) {
        Boolean exists = luaRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    // 비동기 처리 로그인 지연 되면 안되기 때문에
    @Async
    @Override
    public void restoreCartOnLogin(Long memberId) {
        if (memberId == null) return;

        String key = getRedisKey(memberId, null);
        try {
            // 덮어쓰기 방지
            if (hasKey(key)) return;

            loadFromDbAndRestoreToRedisIfAbsent(memberId, key);
        } catch (Exception e) {
            // 예외는 그냥 삼킴 -> 로그인 방해 안하기 위해서
            log.error("로그인 시 장바구니 복구 실패 (MemberId: {})", memberId, e);
        }
    }

    // redis 비어 있을 때 redis에 db 정보 올리는 메서드
    private Map<Object, Object> loadFromDbAndRestoreToRedisIfAbsent(Long memberId, String key) {
        // 이미 존재하면 아무것도 하지 않음 (이게 kick)
        if (hasKey(key)) {
            return luaRedisTemplate.opsForHash().entries(key);
        }

        // db도 비었으면 빈 맵 반환 -> 종료
        List<CartItem> dbItems = cartItemRepository.findByCart_Member_Id(memberId);
        if (dbItems.isEmpty()) return Collections.emptyMap();

        // data Map에 저장
        Map<String, String> restoreData = new HashMap<>();
        for (CartItem item : dbItems) {
            restoreData.put(String.valueOf(item.getBookId()), String.valueOf(item.getQuantity()));
        }

        // redis에 적재 ttl 초기화
        luaRedisTemplate.opsForHash().putAll(key, restoreData);
        touchTtl(key);

        return new HashMap<>(restoreData);
    }

    // 장바구니 추가
    @Override
    public CartAddResponse addToCart(CartAddRequest request, Long memberId, String guestId) {
        String key = getRedisKey(memberId, guestId);

        if (!hasKey(key) && memberId != null) {
            loadFromDbAndRestoreToRedisIfAbsent(memberId, key);
        }

        String field = String.valueOf(request.bookId());

        if (request.quantity() <= 0) throw new BusinessException(ErrorCode.INVALID_QUANTITY);

        Long newQty;
        // Lua script (증가 연산, ttl 초기화, race condition 해결)
        try {
            newQty = luaRedisTemplate.execute(
                    cartUpsertScript,
                    List.of(key),
                    field,
                    String.valueOf(request.quantity()),
                    String.valueOf(MAX_CART_QUANTITY),
                    String.valueOf(CART_TTL_SECONDS),
                    "ADD"
            );
        } catch (Exception e) {
            log.error("Redis Lua error during addToCart", e);
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }

        if (newQty == null) throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        if (newQty == -1) throw new BusinessException(ErrorCode.INVALID_QUANTITY);

        return new CartAddResponse(key, request.bookId(), newQty.intValue());
    }

    // 수량 변경
    @Override
    public CartUpdateResponse updateCartItemQuantity(Long memberId, String guestId, CartItemUpdateRequest request) {
        String key = getRedisKey(memberId, guestId);

        if (!hasKey(key) && memberId != null) {
            loadFromDbAndRestoreToRedisIfAbsent(memberId, key);
        }

        String field = String.valueOf(request.bookId());

        int qty = request.quantity();
        if (qty <= 0 || qty > MAX_CART_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        // 없는 책에 대한 수량을 증가하려 할 때 예외처리
        if (!Boolean.TRUE.equals(luaRedisTemplate.opsForHash().hasKey(key, field))) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        Long newQty;
        // key를 리스트로 보내는 이유 lua가 그렇게 받도록 설계되어있음
        try {
            newQty = luaRedisTemplate.execute(
                    cartUpsertScript,
                    List.of(key),
                    field,
                    String.valueOf(qty),
                    String.valueOf(MAX_CART_QUANTITY),
                    String.valueOf(CART_TTL_SECONDS),
                    "SET"
            );
        } catch (Exception e) {
            log.error("Redis Lua error during updateCartItemQuantity", e);
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }

        if (newQty == null) throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        if (newQty == -1) throw new BusinessException(ErrorCode.INVALID_QUANTITY);

        return new CartUpdateResponse(key, request.bookId(), newQty.intValue());
    }

    // 장바구니 삭제 * lua를 사용 안 한 이유? -> 두번 삭제해봤자 어차피 삭제 된건 똑같음 멱등
    @Override
    public void deleteCartItem(Long memberId, String guestId, Long bookId) {
        String key = getRedisKey(memberId, guestId);

        try {
            luaRedisTemplate.opsForHash().delete(key, String.valueOf(bookId));

            // 장바구니에 아이템 남았는지 체크
            if (Boolean.TRUE.equals(luaRedisTemplate.hasKey(key))) {
                touchTtl(key);
            }else{
                luaRedisTemplate.opsForHash().put(key, "CART_STATUS", "EMPTY");
                touchEmptyTtl(key);
            }
        } catch (Exception e) {
            log.error("Redis error during deleteCartItem", e);
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }
    }

    // 장바구니 비우기
    @Override
    @Transactional
    public void deleteAllCartItem(Long memberId, String guestId, boolean isOrder) {
        String key = getRedisKey(memberId, guestId);
        try {
            Set<Object> fields = luaRedisTemplate.opsForHash().keys(key);
            if (!fields.isEmpty()) {
                luaRedisTemplate.opsForHash().delete(key, fields.toArray());
            }
            luaRedisTemplate.opsForHash().put(key, "CART_STATUS", "EMPTY");
            // 3시간
            touchEmptyTtl(key);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }
    }

    // 장바구니 지우기 주문용 (DB 삭제)
    @Override
    @Transactional
    public void deleteAllCartItemForOrder(Long memberId) {
        String key = getRedisKey(memberId, null);
        try {
            luaRedisTemplate.delete(key);
            if(memberId != null) {
                cartItemRepository.deleteByCart_Member_Id(memberId);
            }
        } catch (Exception e) {
            log.error("주문 후 장바구니 삭제 실패: memberId={}", memberId, e);
        }
    }

    // 장바구니 병합
    @Override
    public void migrateGuestCart(String guestId, Long memberId) {
        // 비회원 장바구니 없거나 회원 아니면 바로 리턴 (방어 코드)
        if (guestId == null || guestId.isBlank() || memberId == null) return;

        String guestKey = guestKey(guestId);
        String memberKey = memberKey(memberId);

        try {
            // 병합 할 내용 없으면 종료
            if (!hasKey(guestKey)) return;

            // redis miss 났을 때 db에서 올림 but 덮어쓰기 방지 (이 함수에서 처리 되있음)
            loadFromDbAndRestoreToRedisIfAbsent(memberId, memberKey);

            Long ok = luaRedisTemplate.execute(
                    cartMergeScript,
                    List.of(memberKey, guestKey), // keys[1] 결과 담을 memberKey, keys[2] 합칠 guestKey
                    String.valueOf(MAX_CART_QUANTITY),
                    String.valueOf(CART_TTL_SECONDS)
            );

            if (ok == null) throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);

            touchTtl(memberKey);
            luaRedisTemplate.delete(guestKey);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Redis Lua error during guest cart migration", e);
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }
    }

    // guestCart만 바로 삭제 할 수 있는 메서드
    @Override
    public void deleteGuestCartOnly(String guestId) {
        if (guestId == null || guestId.isBlank()) return;
        String key = getRedisKey(null, guestId);
        try {
            luaRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis error during deleteGuestCartOnly", e);
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }
    }

    // 장바구니의 리스트를 보여주는 함수
    // Transactional 쓰기 의도 없음 loadFromDbAndRestoreToRedisIfAbsent 메서드 때문
    @Override
    @Transactional(readOnly = true)
    public CartListResponse getCartItemList(Long memberId, String guestId) {
        boolean hasGuestCart = false;

        // 방어 로직 빈 리스트 반환
        if (memberId == null && (guestId == null || guestId.isBlank())) {
            return new CartListResponse(Collections.emptyList(), 0L, false);
        }

        String key = getRedisKey(memberId, guestId);

        try {
            // 일단 redis 조회
            Map<Object, Object> redisItems = luaRedisTemplate.opsForHash().entries(key);

            // 회원이라면 Redis가 비어있을 때만 DB로 복구
            if ((redisItems == null || redisItems.isEmpty()) && memberId != null) {
                redisItems = loadFromDbAndRestoreToRedisIfAbsent(memberId, key);
            }

            // 로그인 했는데 아직 비회원 장바구니 남아있을 때
            if (memberId != null && guestId != null && !guestId.isBlank()) {
                hasGuestCart = hasKey(getRedisKey(null, guestId));
            }

            // 그래도 비어 있으면 바로 반환
            if (redisItems == null || redisItems.isEmpty()) {
                return new CartListResponse(Collections.emptyList(), 0L, hasGuestCart);
            }

            if (redisItems != null && !redisItems.isEmpty()) {
                touchTtl(key);
            }

            return calculateCartResponse(redisItems, hasGuestCart, key);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Unexpected error in getCartItemList", e);
            throw new BusinessException(ErrorCode.REDIS_SERVER_ERROR);
        }
    }

    // 장바구니 보여줄 때 전체적으로 계산해서 보여줄 헬퍼 메서드
    private CartListResponse calculateCartResponse(Map<Object, Object> redisItems, boolean hasGuestCart, String redisKey) {
        // 외부 API 호출용
        List<Long> bookIds = new ArrayList<>();
        Map<Long, Integer> quantityMap = new HashMap<>();

        // Redis 엔트리 파싱
        for (Map.Entry<Object, Object> entry : redisItems.entrySet()) {
            try {
                Long bookId = Long.valueOf(String.valueOf(entry.getKey()));
                int quantity = parseQuantity(entry.getValue());
                if (quantity <= 0) continue;

                bookIds.add(bookId);
                quantityMap.put(bookId, quantity);
            } catch (NumberFormatException e) {
                log.warn("엔트리 파싱 NumberFormatException");
            }
        }

        // 책 id가 없다? 바로 종료
        if (bookIds.isEmpty()) {
            return new CartListResponse(Collections.emptyList(), 0L, hasGuestCart);
        }

        // feign 호출
        List<GetBookResponse> bookInfos = bookFeignClient.getBooksBulk(bookIds);
        if (bookInfos == null) bookInfos = Collections.emptyList();

        // 책 서버에 존재하는 id만 추리는 작업
        Set<Long> foundBookIds = bookInfos.stream()
                .filter(Objects::nonNull)
                .map(GetBookResponse::bookId)
                .collect(Collectors.toSet());

        // 삭제된 상품 정리
        List<Long> ghostItemIds = bookIds.stream()
                .filter(id -> !foundBookIds.contains(id))
                .toList();

        if (!ghostItemIds.isEmpty()) {
            log.warn("유효하지 않은 상품 발견(삭제됨): {}. 장바구니에서 제거합니다.", ghostItemIds);
            luaRedisTemplate.opsForHash().delete(redisKey, ghostItemIds.toArray());
        }

        // 계산 로직
        List<CartDetailResponse> cartDetails = new ArrayList<>();
        long totalPrice = 0L;

        for (GetBookResponse book : bookInfos) {
            if (book == null) continue;

            int quantity = quantityMap.getOrDefault(book.bookId(), 0);
            int price = (book.price() != null) ? book.price() : 0;

            int itemTotalPrice = price * quantity;
            cartDetails.add(new CartDetailResponse(
                    book.bookId(),
                    book.title(),
                    price,
                    quantity,
                    itemTotalPrice,
                    book.image()
            ));
            totalPrice += itemTotalPrice;
        }

        return new CartListResponse(cartDetails, totalPrice, hasGuestCart);
    }

    // redis 스트링 데이터를 계산할 수 있도록 Integer로 반환해주는 헬퍼 메서드
    private int parseQuantity(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse quantity: {}", value);
            return 0;
        }
    }

    // 스케줄러 db 업데이트 메서드 + redis 삭제까지 진행
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 무조건 새 트랜잭션 열고 독립적!
    public void syncToDb(Long memberId, Map<Object, Object> redisItems) {
        // 비회원 접근 x
        if (memberId == null) return;

        String cartKey = getRedisKey(memberId, null);
        String lockKey = getSyncLockKey(memberId);

        Boolean acquired = luaRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            log.info("동기화 작업 중복 감지됨 (Skip). MemberId: {}", memberId);
            return;
        }

        // member 존재하는지
        try {
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member == null) {
                log.warn("DB Sync failed: Member not found (ID: {})", memberId);
                return;
            }

            // Redis를 Map 형식으로 변환하는 작업 (책 Id, 수량)
            Map<Long, Integer> redisMap = new HashMap<>();
            if (redisItems != null) {
                for (Map.Entry<Object, Object> entry : redisItems.entrySet()) {
                    String keyStr = String.valueOf(entry.getKey());
                    // 빈 장바구니 처리
                    if ("CART_STATUS".equals(keyStr)) {
                        continue;
                    }

                    try {
                        Long bookId = Long.parseLong(String.valueOf(entry.getKey()));
                        int quantity = Integer.parseInt(String.valueOf(entry.getValue()));
                        if (quantity > 0) {
                            redisMap.put(bookId, Math.min(quantity, MAX_CART_QUANTITY));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Skipped MemberId: {}, Key: {}, Value: {}", memberId, entry.getKey(), entry.getValue());
                    }
                }
            }

            Cart cart = cartRepository.findByMember_Id(memberId)
                    .orElseGet(() -> cartRepository.save(new Cart(member)));

            List<CartItem> dbItems = cartItemRepository.findByCart_Member_Id(memberId);

            List<CartItem> toDelete = new ArrayList<>();

            for (CartItem dbItem : dbItems) {
                Long bookId = dbItem.getBookId();
                // remove 해당 삭제 내용 반환
                Integer redisQty = redisMap.remove(bookId); // 만약 해당하는 책 Id 없으면 null -> db 삭제
                if (redisQty != null) {
                    // 삭제되면 db에 업데이트 해야하는 내용
                    if (dbItem.getQuantity() != redisQty) {
                        dbItem.updateQuantity(redisQty);
                    }
                } else {
                    toDelete.add(dbItem);
                }
            }

            // 삭제는 한번에 batch로 처리
            if (!toDelete.isEmpty()) {
                cartItemRepository.deleteAllInBatch(toDelete);
            }

            // 신규 아이템 insert
            if (!redisMap.isEmpty()) {
                List<CartItem> newItems = redisMap.entrySet().stream()
                        .map(entry -> new CartItem(entry.getKey(), entry.getValue(), cart))
                        .toList();
                cartItemRepository.saveAll(newItems);
            }

            luaRedisTemplate.delete(cartKey);
        }catch (Exception e) {
            log.error("DB Sync 중 에러 발생: memberId={}", memberId, e);
            throw e;
        } finally {
            luaRedisTemplate.delete(lockKey);
        }
    }

    // 로그아웃 시 사용할 껍데기 메서드
    @Override
    public void syncToDb(Long memberId) {
        if (memberId == null) return;

        String key = getRedisKey(memberId, null);

        if (!hasKey(key)) {
            log.info("Redis Key 만료됨 (또는 없음). DB 동기화 스킵. MemberId: {}", memberId);
            return;
        }

        Map<Object, Object> redisItems = luaRedisTemplate.opsForHash().entries(key);

        syncToDb(memberId, redisItems);
    }
}
