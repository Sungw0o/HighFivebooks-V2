package com.nhnacademy.member_server.listener;

import com.nhnacademy.member_server.dto.event.MemberLoginEvent;
import com.nhnacademy.member_server.dto.event.MemberLogoutEvent;
import com.nhnacademy.member_server.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventListener {

    private final CartService cartService;

    @Async
    @EventListener
    public void handleLoginEvent(MemberLoginEvent event) {
        Long memberId = event.getMemberId();
        if (memberId == null) return;

        try {
            log.info("로그인 감지: 장바구니 복구 시작 (MemberId: {})", memberId);
            cartService.restoreCartOnLogin(memberId);
        } catch (Exception e) {
            log.error("로그인 후 장바구니 복구 실패: memberId={}", memberId, e);
        }
    }

    @Async
    @EventListener
    public void handleLogoutEvent(MemberLogoutEvent event) {
        Long memberId = event.getMemberId();
        if (memberId == null) return;

        try {
            log.info("로그아웃 감지: 장바구니 DB 동기화 시작 (MemberId: {})", memberId);
            cartService.syncToDb(memberId);
        } catch (Exception e) {
            log.error("로그아웃 후 장바구니 동기화 실패: memberId={}", memberId, e);
        }
    }
}