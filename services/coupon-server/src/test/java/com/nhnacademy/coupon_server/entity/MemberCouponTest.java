package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberCouponTest {

    @Test
    @DisplayName("쿠폰 사용 가능 검증 성공 - 정상 상태(ISSUED) 및 기간 내")
    void validateUsable_Success() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1)) // 미래 날짜
                .build();

        assertThatCode(coupon::validateUsable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 성공 - 만료일이 없는 경우 (Null Safe)")
    void validateUsable_Success_NoExpiration() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(null) // 만료일 없음
                .build();

        assertThatCode(coupon::validateUsable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 실패 - 이미 사용된 쿠폰 (USED)")
    void validateUsable_Fail_Used() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.USED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용했거나 사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 실패 - 상태가 만료됨 (EXPIRED)")
    void validateUsable_Fail_StatusExpired() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.EXPIRED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용했거나 사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 실패 - 유효 기간 지남 (Date check)")
    void validateUsable_Fail_DateExpired() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().minusSeconds(1)) // 과거 날짜
                .build();

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("유효 기간이 지난 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 취소 성공 - 상태가 ISSUED로 변경되고 사용 정보가 초기화되어야 함")
    void cancel_Success() {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .status(Status.USED)           // 사용된 상태
                .usedAt(LocalDateTime.now())   // 사용 일시 존재
                .orderId(12345L)               // 주문 번호 존재
                .build();

        memberCoupon.cancel();

        Assertions.assertEquals(Status.ISSUED, memberCoupon.getStatus(), "상태가 ISSUED로 변경되어야 합니다.");
        Assertions.assertNull(memberCoupon.getUsedAt(), "사용 일시가 null로 초기화되어야 합니다.");
        Assertions.assertNull(memberCoupon.getOrderId(), "주문 ID가 null로 초기화되어야 합니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 취소 실패 - USED 상태가 아닐 때 예외 발생")
    void cancel_Fail_NotUsed() {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .status(Status.ISSUED) // 사용된 상태가 아님 (ISSUED or EXPIRED)
                .build();

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, memberCoupon::cancel);
        Assertions.assertEquals("사용된 상태의 쿠폰만 취소할 수 있습니다.", exception.getMessage());
    }
}