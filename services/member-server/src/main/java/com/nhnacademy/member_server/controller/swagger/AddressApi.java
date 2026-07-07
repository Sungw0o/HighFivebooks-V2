package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.member.AddressRequest;
import com.nhnacademy.member_server.dto.response.member.AddressListResponse;
import com.nhnacademy.member_server.dto.response.member.AddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "3. Address", description = "회원 배송지 관리")
public interface AddressApi {

    @Operation(summary = "기본 배송지 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = AddressResponse.class),
            examples = @ExampleObject(value = """
                    {
                      "addressId": 10,
                      "alias": "우리집",
                      "recipient": "홍길동",
                      "phone": "010-1234-5678",
                      "zipCode": "12345",
                      "roadAddress": "경기 성남시 분당구 대왕판교로 645",
                      "detailAddress": "NHN 플레이뮤지엄",
                      "default": true
                    }
                    """)))
    ResponseEntity<AddressResponse> getDefaultAddress(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId);

    @Operation(summary = "모든 배송지 조회")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AddressListResponse.class),
            examples = @ExampleObject(value = """
                    {
                      "addressList": [
                        {
                          "addressId": 10,
                          "alias": "우리집",
                          "recipient": "홍길동",
                          "phone": "010-1234-5678",
                          "zipCode": "12345",
                          "roadAddress": "경기 성남시...",
                          "detailAddress": "101호",
                          "default": true
                        },
                        {
                          "addressId": 15,
                          "alias": "회사",
                          "recipient": "홍과장",
                          "phone": "010-9876-5432",
                          "zipCode": "54321",
                          "roadAddress": "서울 구로구...",
                          "detailAddress": "사무실",
                          "default": false
                        }
                      ]
                    }
                    """)))
    ResponseEntity<AddressListResponse> getAddressList(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId);

    @Operation(summary = "특정 배송지 조회")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AddressResponse.class)))
    ResponseEntity<AddressResponse> getAddress(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @PathVariable("address-id") Long addressId);

    @Operation(summary = "배송지 추가")
    @ApiResponse(responseCode = "200", description = "추가된 배송지 정보 반환", content = @Content(schema = @Schema(implementation = AddressResponse.class)))
    ResponseEntity<AddressResponse> createAddress(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @RequestBody AddressRequest addressRequest);

    @Operation(summary = "기본 배송지 설정")
    @ApiResponse(responseCode = "200", description = "설정 완료된 배송지 정보", content = @Content(schema = @Schema(implementation = AddressResponse.class)))
    ResponseEntity<AddressResponse> setDefaultAddress(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @PathVariable("address-id") Long addressId);

    @Operation(summary = "배송지 수정")
    @ApiResponse(responseCode = "200", description = "수정된 배송지 정보", content = @Content(schema = @Schema(implementation = AddressResponse.class)))
    ResponseEntity<AddressResponse> updateAddress(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @RequestBody AddressRequest addressRequest,
            @PathVariable("address-id") Long addressId);

    @Operation(summary = "배송지 삭제")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    ResponseEntity<Void> deleteAddress(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @PathVariable("address-id") Long addressId);
}