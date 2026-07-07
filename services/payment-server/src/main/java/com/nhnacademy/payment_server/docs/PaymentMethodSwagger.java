package com.nhnacademy.payment_server.docs;

import com.nhnacademy.payment_server.dto.request.MethodStatusRequest;
import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "PaymentMethod API", description = "결제 수단 관련 API")
public interface PaymentMethodSwagger {

    @Operation(summary = "전체 결제 수단 조회", description = "모든 결제 수단을 조회합니다. (프론트에서 isActive 필드로 UI 구분)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (전체 결제수단 리스트 반환)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<List<PaymentMethodResponse>> getAllMethods();


    @Operation(summary = "[관리자] 결제 수단 활성/비활성 토글", description = "결제 수단의 사용 가능 여부를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 결제 수단 ID"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<Void> updateStatus(
            @Parameter(description = "변경할 결제 수단 ID", example = "1") @PathVariable Long methodId,
            @RequestBody MethodStatusRequest request
    );
}
