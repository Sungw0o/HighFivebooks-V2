package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.member.LoginRequest;
import com.nhnacademy.member_server.dto.response.member.TokenDto;
import com.nhnacademy.member_server.service.member.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    AuthController authController;

    @Mock
    AuthService authService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginTest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testUser", "password123!");
        TokenDto tokenDto = new TokenDto("accessToken", "refreshToken", true);

        given(authService.loginUser(any(), any())).willReturn(tokenDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.profileComplete").value(true));
    }

    @Test
    @DisplayName("토큰 재발급 테스트")
    void reissueTest() throws Exception {
        TokenDto tokenDto = new TokenDto("newAccess", "newRefresh", true);

        given(authService.reissue("validRefreshToken")).willReturn(tokenDto);

        mockMvc.perform(post("/api/auth/reissue")
                        .header("X-Refresh-Token", "validRefreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccess"));
    }

    @Test
    @DisplayName("로그아웃 테스트")
    void logoutTest() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-User-ID", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer validToken"))
                .andExpect(status().isOk());

        verify(authService).logout("validToken", 1L);
    }

    @Test
    @DisplayName("소셜 로그인 테스트")
    void loginSocialTest() throws Exception {
        TokenDto tokenDto = new TokenDto("socialAccess", "socialRefresh", false);

        given(authService.loginSocial("PAYCO", "authCode")).willReturn(tokenDto);

        mockMvc.perform(post("/api/auth/login/{provider}", "PAYCO")
                        .param("code", "authCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("socialAccess"));
    }
}