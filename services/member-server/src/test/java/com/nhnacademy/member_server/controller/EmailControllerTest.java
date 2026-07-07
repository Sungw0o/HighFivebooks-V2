package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.member.EmailRequest;
import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.service.member.EmailService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @InjectMocks
    EmailController emailController;

    @Mock
    EmailService emailService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // standaloneSetup 사용 시 String 반환형은 기본적으로 ISO-8859-1로 처리되므로
        // UTF-8 컨버터를 명시적으로 등록해야 합니다.
        mockMvc = MockMvcBuilders.standaloneSetup(emailController)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8), new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("회원가입 인증코드 전송 테스트")
    void sendSignupCodeTest() throws Exception {
        EmailRequest request = new EmailRequest("test@test.com");

        mockMvc.perform(post("/api/emails/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailService).sendVerificationCode("test@test.com", EmailType.SIGNUP);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증코드 전송 테스트")
    void sendPasswordResetCodeTest() throws Exception {
        EmailRequest request = new EmailRequest("test@test.com");

        mockMvc.perform(post("/api/emails/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailService).sendVerificationCode("test@test.com", EmailType.RESET_PASSWORD);
    }

    @Test
    @DisplayName("아이디 찾기 인증코드 전송 테스트")
    void sendFindIdCodeTest() throws Exception {
        EmailRequest request = new EmailRequest("test@test.com");

        mockMvc.perform(post("/api/emails/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailService).sendVerificationCode("test@test.com", EmailType.FIND_ID);
    }

    @Test
    @DisplayName("휴면 계정 활성화 인증코드 전송 테스트")
    void sendDormantCodeTest() throws Exception {
        EmailRequest request = new EmailRequest("test@test.com");

        mockMvc.perform(post("/api/emails/dormant/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailService).sendVerificationCode("test@test.com", EmailType.ACTIVATE);
    }

    @Test
    @DisplayName("이메일 인증 확인 성공 테스트")
    void verifyEmailSuccessTest() throws Exception {
        given(emailService.verifyCode(any(), any(), any())).willReturn(true);

        mockMvc.perform(post("/api/emails/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\", \"code\":\"123456\", \"type\":\"SIGNUP\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("인증 성공"));
    }

    @Test
    @DisplayName("이메일 인증 확인 실패 테스트")
    void verifyEmailFailTest() throws Exception {
        given(emailService.verifyCode(any(), any(), any())).willReturn(false);

        mockMvc.perform(post("/api/emails/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\", \"code\":\"123456\", \"type\":\"SIGNUP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("인증 실패"));
    }
}