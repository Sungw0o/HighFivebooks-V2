package com.nhnacademy.payment_server.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MethodStatusRequest {
    @JsonProperty("active")
    @Schema(description = "결제수단 상태 (true , false)")
    private boolean isActive;
}