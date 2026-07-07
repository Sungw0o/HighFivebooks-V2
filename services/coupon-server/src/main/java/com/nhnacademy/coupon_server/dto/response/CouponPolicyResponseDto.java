package com.nhnacademy.coupon_server.dto.response;

import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.CouponPolicyBook;
import com.nhnacademy.coupon_server.entity.CouponPolicyCategory;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder
public class CouponPolicyResponseDto {
    private Long id;
    private String name;
    private Comment comment;
    private DiscountType discountType;
    private Long discountValue;
    private Long minOrderValue;
    private Long maxDiscountValue;
    private CouponPolicyStatus status;
    private List<Long> targetBookIds;
    private List<Long> targetCategoryIds;

    public static CouponPolicyResponseDto fromEntity(CouponPolicy policy) {
        List<Long> bookIds = policy.getUsableBooks().stream()
                .map(CouponPolicyBook::getBookId).toList();

        List<Long> categoryIds = policy.getUsableCategories().stream()
                .map(CouponPolicyCategory::getCategoryId).toList();

        return CouponPolicyResponseDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .comment(policy.getComment())
                .discountType(policy.getDiscountType())
                .discountValue(policy.getDiscountValue())
                .minOrderValue(policy.getMinOrderValue())
                .maxDiscountValue(policy.getMaxDiscountValue())
                .status(policy.getStatus())
                .targetBookIds(bookIds)
                .targetCategoryIds(categoryIds)
                .build();
    }

}
