package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.member.MemberCreateRequest;
import com.nhnacademy.member_server.dto.request.member.PasswordResetRequest;
import com.nhnacademy.member_server.entity.member.Gender;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.member.AuthService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @InjectMocks
    AccountController accountController;

    @Mock
    AuthService authService;

    @Mock
    MemberRepository memberRepository;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupTest() throws Exception {
        MemberCreateRequest request = MemberCreateRequest.builder()
                .loginId("testUser")
                .password("password123!")
                .name("홍길동")
                .phone("010-1234-5678")
                .email("test@example.com")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/accounts/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).signup(any(MemberCreateRequest.class));
    }

    @Test
    @DisplayName("아이디 중복 확인 (RequestParam) - 존재함")
    void checkIdRequestParamTest() throws Exception {
        String loginId = "existUser";
        given(memberRepository.existsByLoginId(loginId)).willReturn(true);

        mockMvc.perform(get("/api/accounts/check-id")
                        .param("loginId", loginId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("아이디 중복 확인 (PathVariable) - 존재하지 않음")
    void checkLoginIdPathVariableTest() throws Exception {
        String loginId = "newUser";
        given(memberRepository.existsByLoginId(loginId)).willReturn(false);

        mockMvc.perform(get("/api/accounts/exists/login-id/{loginId}", loginId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("아이디 찾기 검증 성공")
    void findIdVerifyTest() throws Exception {
        String requestJson = "{\"email\":\"test@test.com\", \"code\":\"123456\", \"type\":\"FIND_ID\"}";

        given(authService.findLoginIdByEmail("test@test.com", "123456")).willReturn("te**");

        mockMvc.perform(post("/api/accounts/find/id/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("te**"));
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 성공")
    void resetPasswordTest() throws Exception {
        String requestJson = "{\"loginId\":\"testUser\", \"email\":\"test@test.com\", \"authCode\":\"123456\", \"newPassword\":\"newPw123!\"}";

        mockMvc.perform(post("/api/accounts/find/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        verify(authService).resetPassword(any(PasswordResetRequest.class));
    }
}