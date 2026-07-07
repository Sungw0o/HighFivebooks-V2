package com.nhnacademy.member_server.feign;

import com.nhnacademy.member_server.dto.response.social.PaycoTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

//토큰 전용
@FeignClient(name = "payco-auth", url = "https://id.payco.com")
public interface PaycoAuthFeignClient {

    @PostMapping("/oauth2.0/token")
    PaycoTokenResponse getToken(@RequestParam("grant_type") String grantType,
                                @RequestParam("client_id") String clientId,
                                @RequestParam("client_secret") String clientSecret,
                                @RequestParam("code") String code);

}