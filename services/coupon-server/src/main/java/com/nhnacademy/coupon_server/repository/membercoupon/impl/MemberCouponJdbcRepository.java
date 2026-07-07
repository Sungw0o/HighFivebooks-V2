package com.nhnacademy.coupon_server.repository.membercoupon.impl;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberCouponJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void batchInsertMemberCoupons(List<MemberCoupon> memberCoupons) {
        // Bulk Insert 쿼리 (Entity의 @Column name과 일치해야 함)
        String sql = "INSERT INTO member_coupon (user_id, coupon_id, status, issue_at, expired_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql,
                memberCoupons,
                memberCoupons.size(),
                (ps, memberCoupon) -> {
                    ps.setLong(1, memberCoupon.getUserId());
                    ps.setLong(2, memberCoupon.getCoupon().getId());
                    ps.setString(3, memberCoupon.getStatus().name()); // Enum -> String
                    ps.setTimestamp(4, Timestamp.valueOf(memberCoupon.getIssueAt()));
                    
                    // expiredAt은 Nullable이므로 체크 필요
                    if (memberCoupon.getExpiredAt() != null) {
                        ps.setTimestamp(5, Timestamp.valueOf(memberCoupon.getExpiredAt()));
                    } else {
                        ps.setTimestamp(5, null);
                    }
                });
    }
}