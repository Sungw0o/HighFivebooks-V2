package com.nhnacademy.member_server.repository;

import com.nhnacademy.member_server.entity.cart.CartItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart_Member_Id(Long memberId);

    void deleteByCart_Member_Id(Long memberId);
}
