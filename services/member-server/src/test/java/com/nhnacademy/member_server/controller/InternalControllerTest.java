package com.nhnacademy.member_server.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.scheduler.GradeScheduler;
import com.nhnacademy.member_server.service.member.MemberService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

@ExtendWith(MockitoExtension.class)
class InternalControllerTest {

    @InjectMocks
    InternalController internalController;

    @Mock
    MemberService memberService;

    @Mock
    GradeScheduler gradeScheduler;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalController)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    @DisplayName("회원 권한 변경 테스트")
    void updateMemberRoleTest() throws Exception {
        Long memberId = 1L;
        Role newRole = Role.ADMIN;

        mockMvc.perform(put("/internal/{member-id}/role", memberId)
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("회원(" + memberId + ")의 권한이 " + newRole + "로 변경되었습니다."));

        verify(memberService).updateRole(memberId, newRole);
    }

    @Test
    @DisplayName("등급 산정 강제 실행 테스트")
    void forceCalculateGradesTest() throws Exception {
        mockMvc.perform(post("/internal/grades/calculate"))
                .andExpect(status().isOk())
                .andExpect(content().string("등급 산정 스케줄러 강제 실행 완료"));

        verify(gradeScheduler).updateMemberGrades();
    }
}