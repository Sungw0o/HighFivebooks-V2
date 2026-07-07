package com.nhnacademy.member_server.feign;

import com.nhnacademy.member_server.dto.response.social.PaycoMemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

//회원 정보 조회용
@FeignClient(name = "payco-api", url = "https://apis-payco.krp.toastoven.net")
public interface PaycoApiFeignClient {
    @PostMapping("/payco/friends/find_member_v2.json")
    PaycoMemberResponse getMemberInfo(@RequestHeader("client_id") String clientId,
                                      @RequestHeader("access_token") String accessToken);
}
