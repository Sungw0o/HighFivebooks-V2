package com.nhnacademy.coupon_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "TEAM5-MEMBER-SERVER")
public interface MemberServiceClient {
    @GetMapping("/api/members/birthday")
    List<Long> getBirthdayUserId(@RequestParam("month") int month,
                                 @RequestParam("page") int page,
                                 @RequestParam("size") int size);
}
