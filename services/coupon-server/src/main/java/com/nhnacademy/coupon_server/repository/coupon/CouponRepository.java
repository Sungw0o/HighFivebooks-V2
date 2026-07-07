package com.nhnacademy.coupon_server.repository.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long>, CouponRepositoryCustom {

    @Override
    @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy")
    List<Coupon> findAll();

    @Override
    @Query(value = "SELECT c FROM Coupon c JOIN FETCH c.couponPolicy",
            countQuery = "SELECT COUNT(c) FROM Coupon c")
    Page<Coupon> findAll(Pageable pageable);

}
