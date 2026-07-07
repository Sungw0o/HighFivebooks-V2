package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.AddressApi;
import com.nhnacademy.member_server.dto.request.member.AddressRequest;
import com.nhnacademy.member_server.dto.response.member.AddressListResponse;
import com.nhnacademy.member_server.dto.response.member.AddressResponse;
import com.nhnacademy.member_server.service.member.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController implements AddressApi {

    private final AddressService addressService;


    //해당 멤버 기본 주소 불러오기
    @GetMapping("/default")
    public ResponseEntity<AddressResponse> getDefaultAddress(@RequestHeader(name = "X-User-ID") Long memberId) {
        AddressResponse addressResponse = addressService.findDefaultAddress(memberId);
        return ResponseEntity.ok(addressResponse);
    }

    //해당 멤버 모든 주소 불러오기
    @GetMapping
    public ResponseEntity<AddressListResponse> getAddressList(@RequestHeader(name = "X-User-ID") Long memberId) {
        AddressListResponse addressListResponse = addressService.findAddressList(memberId);
        return ResponseEntity.ok(addressListResponse);
    }

    //특정 주소 정보 불러오기 (address Id 기준)
    @GetMapping("/{address-id}")
    public ResponseEntity<AddressResponse> getAddress(@RequestHeader(name = "X-User-ID") Long memberId,
                                                       @PathVariable("address-id") Long addressId) {
        AddressResponse addressResponse = addressService.findAddress(memberId, addressId);
        return ResponseEntity.ok(addressResponse);
    }


    //주소 추가하기
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@RequestHeader(name = "X-User-ID") Long memberId,
                                                         @RequestBody @Valid AddressRequest addressRequest) {
        AddressResponse addressResponse = addressService.registerAddress(memberId, addressRequest);
        return ResponseEntity.ok(addressResponse);
    }

    @PostMapping("/{address-id}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(@RequestHeader(name = "X-User-ID") Long memberId,
                                                             @PathVariable("address-id") Long addressId) {
        AddressResponse addressResponse = addressService.setDefaultAddress(memberId, addressId);
        return ResponseEntity.ok(addressResponse);
    }

    //특정 주소 업데이트
    @PutMapping("/{address-id}")
    public ResponseEntity<AddressResponse> updateAddress(@RequestHeader(name = "X-User-ID") Long memberId,
                                                         @RequestBody @Valid AddressRequest addressRequest,
                                                         @PathVariable("address-id") Long addressId) {
        AddressResponse addressResponse = addressService.modifyAddress(memberId, addressId, addressRequest);
        return ResponseEntity.ok(addressResponse);
    }

    //특정 주소 삭제
    @DeleteMapping("/{address-id}")
    public ResponseEntity<Void> deleteAddress(@RequestHeader(name = "X-User-ID") Long memberId,
                                              @PathVariable("address-id") Long addressId) {
        addressService.removeAddress(memberId, addressId);
        return ResponseEntity.ok().build();
    }

}