package com.nhnacademy.payment_server.docs;

import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Payment API", description = "결제 관련 API")
public interface PaymentSwagger {
    @Operation(summary = "결제 최종 승인", description = "Toss에게 최종 결제 승인을 요청합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 승인 성공 (DONE)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (금액 불일치, 필수값 누락 등)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문 번호 (paymentKey 불일치)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 Toss 결제 실패")
    })
    ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest request);


    @Operation(summary = "결제 취소", description = "승인된 Toss 결제를 취소하고. 사용된 포인트를 롤백합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 취소 성공 (CANCELED)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 취소된 결제, 미완료 결제 등)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 결제 (paymentKey 불일치)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 Toss 취소 실패")
    })
    ResponseEntity<Void> cancelPayment(@PathVariable String paymentKey, @RequestBody PaymentCancelRequest request);

}
