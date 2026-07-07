package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.member.DormantRequest;
import com.nhnacademy.member_server.dto.request.member.MemberUpdateRequest;
import com.nhnacademy.member_server.dto.response.member.MemberResponse;
import com.nhnacademy.member_server.dto.response.member.SimpleMemberResponse;
import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.service.member.AuthService;
import com.nhnacademy.member_server.service.member.EmailService;
import com.nhnacademy.member_server.service.member.MemberService;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @InjectMocks
    MemberController memberController;

    @Mock
    MemberService memberService;

    @Mock
    AuthService authService;

    @Mock
    EmailService emailService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .setMessageConverters(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @Test
    @DisplayName("내 정보 조회 테스트")
    void getMemberTest() throws Exception {
        MemberResponse response = MemberResponse.builder().name("TestUser").build();
        given(memberService.getMember(1L)).willReturn(response);

        mockMvc.perform(get("/api/members/me")
                        .header("X-User-ID", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 정보 수정 테스트")
    void updateMemberTest() throws Exception {
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .name("UpdatedName")
                .build();
        MemberResponse response = MemberResponse.builder().name("UpdatedName").build();

        given(memberService.updateMember(eq(1L), any(MemberUpdateRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/members/me")
                        .header("X-User-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 탈퇴 테스트")
    void withdrawTest() throws Exception {
        mockMvc.perform(delete("/api/members/me/withdraw")
                        .header("X-User-ID", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk());

        verify(authService).logout("token", 1L);
        verify(memberService).withdraw(1L);
    }

    @Test
    @DisplayName("생일자 조회 테스트")
    void getBirthdayMemberIdsTest() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        given(memberService.getBirthdayMemberIds(1)).willReturn(ids);

        mockMvc.perform(get("/api/members/birthday")
                        .param("month", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 권한 변경 테스트")
    void updateMemberRoleTest() throws Exception {
        mockMvc.perform(put("/api/members/{member-id}/role", 1L)
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("회원(1)의 권한이 ADMIN로 변경되었습니다."));

        verify(memberService).updateRole(1L, Role.ADMIN);
    }

    @Test
    @DisplayName("회원 목록 정보 조회 테스트")
    void getMembersInfoTest() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        List<SimpleMemberResponse> responses = Collections.emptyList();

        given(memberService.getMembersInfo(ids)).willReturn(responses);

        mockMvc.perform(post("/api/members/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("휴면 여부 확인 테스트")
    void checkDormantTest() throws Exception {
        DormantRequest request = new DormantRequest();

        mockMvc.perform(post("/api/members/open/dormant/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(memberService).checkDormantMember(request.getLoginId(), request.getEmail());
    }

    @Test
    @DisplayName("휴면 해제 테스트")
    void activateDormantTest() throws Exception {
        String json = "{\"loginId\":\"test\",\"email\":\"t@t.com\",\"authCode\":\"123456\"}";

        given(emailService.verifyCode(any(), any(), eq(EmailType.ACTIVATE))).willReturn(true);

        mockMvc.perform(post("/api/members/open/dormant/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(memberService).activateDormantMember("test", "t@t.com");
    }
}