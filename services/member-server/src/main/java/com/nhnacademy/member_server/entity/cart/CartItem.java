package com.nhnacademy.member_server.entity.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "cart_item",
        // 한 장바구니에 똑같은 책이 중복해서 들어가는 것을 DB 레벨에서 막음
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_book",
                        columnNames = {"cart_id", "book_id"}
                )
        }
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 필수
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    public CartItem(Long bookId, int quantity, Cart cart){
        this.bookId = bookId;
        this.quantity = quantity;
        this.cart = cart;
    }

    // 수량 변경 편의 메서드 (Dirty Checking용)
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}