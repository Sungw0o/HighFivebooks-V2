package com.nhnacademy.payment_server.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/business-error")
        public void throwBusinessException() {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        @GetMapping("/test/runtime-error")
        public void throwRuntimeException() {
            throw new RuntimeException("예상치 못한 에러");
        }
    }

    @Test
    @DisplayName("BusinessException 발생 - 정의된 ErrorCode와 상태코드 반환")
    void handleBusinessException() throws Exception {
        // when & then
        mockMvc.perform(get("/test/business-error"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("일반 Exception 발생 - 500 에러 + P999 코드")
    void handleException() throws Exception {
        // when & then
        mockMvc.perform(get("/test/runtime-error"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("P999"))
                .andExpect(jsonPath("$.message").value("알 수 없는 서버 오류가 발생했습니다."));
    }
}