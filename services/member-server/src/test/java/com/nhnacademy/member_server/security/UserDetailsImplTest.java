package com.nhnacademy.member_server.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.entity.member.Status;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserDetailsImplTest {

    @Test
    void getAuthorities_Success() {
        Member member = Member.builder()
                .role(Role.USER)
                .build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void getPassword_Success() {
        Member member = Member.builder().password("encodedPw").build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        assertThat(userDetails.getPassword()).isEqualTo("encodedPw");
    }

    @Test
    void getUsername_Success() {
        Member member = Member.builder().loginId("testUser").build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        assertThat(userDetails.getUsername()).isEqualTo("testUser");
    }

    @Test
    void isAccountNonLocked_True_WhenActive() {
        Member member = Member.builder().status(Status.ACTIVE).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void isAccountNonLocked_False_WhenDormant() {
        Member member = Member.builder().status(Status.DORMANT).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void isEnabled_True_WhenActive() {
        Member member = Member.builder().status(Status.ACTIVE).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_False_WhenWithdrawal() {
        Member member = Member.builder().status(Status.WITHDRAWAL).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void isAccountNonExpired_AlwaysTrue() {
        UserDetailsImpl userDetails = new UserDetailsImpl(Member.builder().build());
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_AlwaysTrue() {
        UserDetailsImpl userDetails = new UserDetailsImpl(Member.builder().build());
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }
}