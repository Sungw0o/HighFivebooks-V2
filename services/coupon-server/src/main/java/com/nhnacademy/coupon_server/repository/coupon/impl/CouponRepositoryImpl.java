package com.nhnacademy.coupon_server.repository.coupon.impl;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.nhnacademy.coupon_server.entity.QCoupon.coupon;
import static com.nhnacademy.coupon_server.entity.QCouponPolicy.couponPolicy;
import static com.nhnacademy.coupon_server.entity.QCouponPolicyBook.couponPolicyBook;
import static com.nhnacademy.coupon_server.entity.QCouponPolicyCategory.couponPolicyCategory;

public class CouponRepositoryImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public CouponRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public Page<Coupon> findIssuableCoupons(LocalDateTime now, CouponPolicyStatus status, CouponType couponType, Pageable pageable) {
        List<Coupon> content = jpaQueryFactory
                .selectFrom(coupon)
                .join(coupon.couponPolicy, couponPolicy).fetchJoin()
                .where(
                        isIssuableDate(now),
                        couponPolicy.status.eq(status),
                        coupon.status.eq(CouponStatus.ACTIVE),
                        coupon.couponType.eq(couponType)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(coupon.count())
                .from(coupon)
                .join(coupon.couponPolicy, couponPolicy)
                .where(
                        isIssuableDate(now),
                        couponPolicy.status.eq(status),
                        coupon.status.eq(CouponStatus.ACTIVE),
                        coupon.couponType.eq(couponType)
                );
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Coupon> findCouponsForProduct(Long bookId, List<Long> categoryIds, CouponPolicyStatus status, LocalDateTime now) {
        return jpaQueryFactory
                .selectFrom(coupon).distinct()
                .join(coupon.couponPolicy, couponPolicy).fetchJoin()
                .leftJoin(couponPolicy.usableBooks, couponPolicyBook)
                .leftJoin(couponPolicy.usableCategories, couponPolicyCategory)
                .where(
                        couponPolicy.status.eq(status),
                        isIssuableDate(now),
                        // (특정 책 OR 특정 카테고리 OR (책 조건 X AND 카테고리 조건 X))
                        isTargetBook(bookId)
                                .or(isTargetCategory(categoryIds))
                                .or(isGlobalPolicy())
                )
                .fetch();
    }

    @Override
    public List<Coupon> findSpecificCouponsForProduct(Long bookId, List<Long> categoryIds, CouponPolicyStatus status, LocalDateTime now) {
        return jpaQueryFactory
                .selectFrom(coupon).distinct()
                .join(coupon.couponPolicy, couponPolicy).fetchJoin()
                .leftJoin(couponPolicy.usableBooks, couponPolicyBook)
                .leftJoin(couponPolicy.usableCategories, couponPolicyCategory)
                .where(
                        couponPolicy.status.eq(status),
                        isIssuableDate(now),
                        // (특정 책 OR 특정 카테고리) - 범용 제외
                        isTargetBook(bookId)
                                .or(isTargetCategory(categoryIds))
                )
                .fetch();
    }

    @Override
    public List<Coupon> findCouponsByCommentAndStatus(Comment comment, CouponPolicyStatus status, Pageable pageable) {
        return jpaQueryFactory
                .selectFrom(coupon)
                .join(coupon.couponPolicy, couponPolicy).fetchJoin()
                .where(
                        couponPolicy.comment.eq(comment),
                        couponPolicy.status.eq(status)
                )
                .orderBy(coupon.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    // --- Helper Methods (BooleanExpression) ---

    // 발급 가능 기간 체크: (start == null OR start <= now) AND (end == null OR end >= now)
    private BooleanExpression isIssuableDate(LocalDateTime now) {
        return coupon.issuedStartAt.isNull().or(coupon.issuedStartAt.loe(now))
                .and(coupon.issuedEndAt.isNull().or(coupon.issuedEndAt.goe(now)));
    }

    // 특정 도서 대상인지
    private BooleanExpression isTargetBook(Long bookId) {
        if (bookId == null) return couponPolicyBook.bookId.isNull();
        return couponPolicyBook.bookId.eq(bookId);
    }

    // 특정 카테고리 대상인지
    private BooleanExpression isTargetCategory(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return couponPolicyCategory.categoryId.isNull();
        return couponPolicyCategory.categoryId.in(categoryIds);
    }

    // 범용 정책인지 (책 조건 NULL AND 카테고리 조건 NULL)
    private BooleanExpression isGlobalPolicy() {
        return JPAExpressions
                .selectFrom(couponPolicyBook)
                .where(couponPolicyBook.couponPolicy.eq(couponPolicy))
                .notExists()
                .and(
                        JPAExpressions
                                .selectFrom(couponPolicyCategory)
                                .where(couponPolicyCategory.couponPolicy.eq(couponPolicy))
                                .notExists()
                );
    }
}
