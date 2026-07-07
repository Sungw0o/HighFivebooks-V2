package com.nhnacademy.member_server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailServiceImplTest {

    @InjectMocks
    UserDetailServiceImpl userDetailService;

    @Mock
    MemberRepository memberRepository;

    @Test
    void loadUserByUsername_Success() {
        String loginId = "testUser";
        Member member = Member.builder()
                .id(1L)
                .loginId(loginId)
                .password("encodedPw")
                .role(Role.USER)
                .build();

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));

        UserDetails userDetails = userDetailService.loadUserByUsername(loginId);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(loginId);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPw");
    }

    @Test
    void loadUserByUsername_Fail_NotFound() {
        String loginId = "unknown";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailService.loadUserByUsername(loginId))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}