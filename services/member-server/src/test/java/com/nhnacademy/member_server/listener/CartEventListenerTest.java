package com.nhnacademy.member_server.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.dto.event.MemberLoginEvent;
import com.nhnacademy.member_server.dto.event.MemberLogoutEvent;
import com.nhnacademy.member_server.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartEventListenerTest {

    @InjectMocks
    private CartEventListener cartEventListener;

    @Mock
    private CartService cartService;

    // ==========================================
    // 1. handleLoginEvent (로그인 이벤트 테스트)
    // ==========================================

    @Test
    @DisplayName("로그인 이벤트 - 정상적으로 장바구니 복구 서비스 호출")
    void handleLoginEvent_Success() {
        // given
        Long memberId = 1L;
        MemberLoginEvent event = new MemberLoginEvent(memberId);

        // when
        cartEventListener.handleLoginEvent(event);

        // then
        // cartService.restoreCartOnLogin(1L)이 정확히 1번 호출되었는지 검증
        verify(cartService, times(1)).restoreCartOnLogin(memberId);
    }

    @Test
    @DisplayName("로그인 이벤트 - MemberId가 null이면 서비스 호출 없이 종료")
    void handleLoginEvent_NullMemberId() {
        // given
        MemberLoginEvent event = new MemberLoginEvent(null);

        // when
        cartEventListener.handleLoginEvent(event);

        // then
        // 서비스 메서드가 절대 호출되지 않았음을 검증 (never)
        verify(cartService, never()).restoreCartOnLogin(anyLong());
    }

    @Test
    @DisplayName("로그인 이벤트 - 서비스 실행 중 예외가 발생해도 삼키고 로그만 남김")
    void handleLoginEvent_ExceptionHandled() {
        // given
        Long memberId = 1L;
        MemberLoginEvent event = new MemberLoginEvent(memberId);

        // 서비스가 예외를 던지도록 설정 (void 메서드는 willThrow 사용)
        willThrow(new RuntimeException("Redis connection failed"))
                .given(cartService).restoreCartOnLogin(memberId);

        // when & then
        // 예외가 밖으로 던져지지 않음을 검증 (assertThatCode().doesNotThrowAnyException())
        assertThatCode(() -> cartEventListener.handleLoginEvent(event))
                .doesNotThrowAnyException();

        // 예외는 났지만, 호출 시도 자체는 있었는지 확인
        verify(cartService).restoreCartOnLogin(memberId);
    }

    // ==========================================
    // 2. handleLogoutEvent (로그아웃 이벤트 테스트)
    // ==========================================

    @Test
    @DisplayName("로그아웃 이벤트 - 정상적으로 DB 동기화 서비스 호출")
    void handleLogoutEvent_Success() {
        // given
        Long memberId = 100L;
        MemberLogoutEvent event = new MemberLogoutEvent(memberId);

        // when
        cartEventListener.handleLogoutEvent(event);

        // then
        verify(cartService, times(1)).syncToDb(memberId);
    }

    @Test
    @DisplayName("로그아웃 이벤트 - MemberId가 null이면 서비스 호출 없이 종료")
    void handleLogoutEvent_NullMemberId() {
        // given
        MemberLogoutEvent event = new MemberLogoutEvent(null);

        // when
        cartEventListener.handleLogoutEvent(event);

        // then
        verify(cartService, never()).syncToDb(anyLong());
    }

    @Test
    @DisplayName("로그아웃 이벤트 - 서비스 실행 중 예외가 발생해도 삼키고 로그만 남김")
    void handleLogoutEvent_ExceptionHandled() {
        // given
        Long memberId = 100L;
        MemberLogoutEvent event = new MemberLogoutEvent(memberId);

        // 서비스 예외 발생 Stubbing
        willThrow(new RuntimeException("DB Connection timeout"))
                .given(cartService).syncToDb(memberId);

        // when & then
        // 리스너가 예외를 catch 블록에서 잡았으므로, 테스트는 실패하지 않아야 함
        assertThatCode(() -> cartEventListener.handleLogoutEvent(event))
                .doesNotThrowAnyException();

        verify(cartService).syncToDb(memberId);
    }
}