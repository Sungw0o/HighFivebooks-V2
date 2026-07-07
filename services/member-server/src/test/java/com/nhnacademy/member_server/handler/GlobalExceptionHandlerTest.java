package com.nhnacademy.member_server.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false) // [핵심 수정] Security Filter 비활성화 -> 401 에러 해결
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class Config {
        // 테스트용 핸들러 빈 등록
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    // 테스트를 위한 가짜 컨트롤러
    @RestController
    static class TestController {
        @GetMapping("/test/business")
        public void throwBusiness() {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        @GetMapping("/test/validation")
        public void throwValidation() throws MethodArgumentNotValidException {
            // Validation 예외 강제 생성
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("object", "field", "must not be null");

            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
            when(ex.getBindingResult()).thenReturn(bindingResult);
            throw ex;
        }

        @GetMapping("/test/unknown")
        public void throwUnknown() {
            throw new RuntimeException("Unexpected Error");
        }

        @GetMapping("/test/feign-404")
        public void throwFeign404() {
            Request request = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, null, null);
            throw new FeignException.NotFound("Not Found", request, null, null);
        }

        @GetMapping("/test/feign-500")
        public void throwFeign500() {
            Request request = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, null, null);
            throw new FeignException.InternalServerError("Server Error", request, null, null);
        }

        @GetMapping("/test/retryable")
        public void throwRetryable() {
            Request request = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, null, null);
            throw new RetryableException(503, "Unavailable", Request.HttpMethod.GET, (Long) null, request);
        }
    }

    @Test
    @DisplayName("BusinessException 처리 테스트")
    void handleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound()) // MEMBER_NOT_FOUND is 404
                .andExpect(jsonPath("$.code").value("M001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("Validation Exception 처리 테스트")
    void handleValidationException() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("must not be null"))
                .andDo(print());
    }

    @Test
    @DisplayName("알 수 없는 Exception 처리 테스트")
    void handleException() throws Exception {
        mockMvc.perform(get("/test/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("C002"))
                .andExpect(jsonPath("$.message").value("알 수 없는 서버 오류가 발생했습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("Feign Exception (404) 처리 테스트")
    void handleFeignException_404() throws Exception {
        mockMvc.perform(get("/test/feign-404"))
                .andExpect(status().isNotFound()) // BOOK_NOT_FOUND_IN_SERVER status
                .andExpect(jsonPath("$.code").value("EXT002"))
                .andDo(print());
    }

    @Test
    @DisplayName("Feign Exception (그 외) 처리 테스트")
    void handleFeignException_500() throws Exception {
        mockMvc.perform(get("/test/feign-500"))
                .andExpect(status().isServiceUnavailable()) // EXTERNAL_SERVER_ERROR status
                .andExpect(jsonPath("$.code").value("EXT001"))
                .andDo(print());
    }

    @Test
    @DisplayName("RetryableException (Connection Refused 등) 처리 테스트")
    void handleRetryableException() throws Exception {
        mockMvc.perform(get("/test/retryable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXT001"))
                .andExpect(jsonPath("$.message").value("현재 도서 서비스를 이용할 수 없습니다."))
                .andDo(print());
    }
}