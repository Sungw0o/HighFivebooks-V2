package com.nhnacademy.coupon_server.repository.membercoupon.impl;

import com.nhnacademy.coupon_server.dto.response.CouponCountDto;
import com.nhnacademy.coupon_server.dto.response.CouponCountVo;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.nhnacademy.coupon_server.entity.QCoupon.coupon;
import static com.nhnacademy.coupon_server.entity.QCouponPolicy.couponPolicy;
import static com.nhnacademy.coupon_server.entity.QMemberCoupon.memberCoupon;

@RequiredArgsConstructor
public class MemberCouponRepositoryImpl implements MemberCouponRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberCoupon> findUsableCoupons(Long userId, LocalDateTime now) {
        return queryFactory
                .selectFrom(memberCoupon)
                .join(memberCoupon.coupon, coupon).fetchJoin()
                .join(coupon.couponPolicy, couponPolicy).fetchJoin()
                .where(
                        memberCoupon.userId.eq(userId),
                        memberCoupon.status.eq(Status.ISSUED),
                        memberCoupon.expiredAt.after(now)
                )
                .fetch();
    }

    @Override
    public List<CouponCountDto> countByCouponIds(List<Long> couponIds) {
        return queryFactory
                .select(Projections.constructor(CouponCountVo.class,
                        memberCoupon.coupon.id,
                        memberCoupon.count()
                ))
                .from(memberCoupon)
                .where(memberCoupon.coupon.id.in(couponIds))
                .groupBy(memberCoupon.coupon.id)
                .fetch()
                .stream()
                .map(vo -> (CouponCountDto) vo) // 인터페이스로 캐스팅
                .toList();
    }

    @Override
    public Page<MemberCoupon> findMemberCouponsByUserId(Long userId, Pageable pageable) {
        List<MemberCoupon> content = queryFactory
                .selectFrom(memberCoupon)
                .join(memberCoupon.coupon, coupon).fetchJoin() // 쿠폰 정보는 보통 같이 필요하므로 페치 조인
                .where(memberCoupon.userId.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(memberCoupon.issueAt.desc()) // 기본 정렬 (필요 시 Sort 객체 동적 적용 가능)
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(memberCoupon.count())
                .from(memberCoupon)
                .where(memberCoupon.userId.eq(userId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
