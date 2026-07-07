package com.nhnacademy.member_server.repository;

import com.nhnacademy.member_server.entity.cart.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByMember_Id(Long memberId);
    void deleteByMemberId(Long memberId);
}
