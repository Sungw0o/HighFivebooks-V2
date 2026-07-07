package com.nhnacademy.member_server.service.impl.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.dto.request.member.MemberUpdateRequest;
import com.nhnacademy.member_server.dto.response.member.MemberResponse;
import com.nhnacademy.member_server.dto.response.member.SimpleMemberResponse;
import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.utils.Sha256Utils;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @InjectMocks
    MemberServiceImpl memberService;

    @Mock
    MemberRepository memberRepository;
    @Mock
    Sha256Utils sha256Utils;


    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_Success() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).status(Status.ACTIVE).build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberService.withdraw(memberId);

        assertThat(member.getStatus()).isEqualTo(Status.WITHDRAWAL);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
    void withdraw_Fail_NotFound() {
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.withdraw(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }


    @Test
    @DisplayName("회원 조회 성공")
    void getMember_Success() {
        Long memberId = 1L;
        Member member = Member.builder()
                .id(memberId)
                .name("Test")
                .email("test@test.com")
                .phone("01012345678")
                .birthDate(LocalDate.now())
                .status(Status.ACTIVE)
                .grade(Grade.builder().gradeName("GENERAL").build())
                .build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        MemberResponse response = memberService.getMember(memberId);

        assertThat(response.getName()).isEqualTo("Test");
    }

    @Test
    @DisplayName("회원 조회 실패 - 존재하지 않는 회원")
    void getMember_Fail_NotFound() {
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }


    @Test
    @DisplayName("회원 정보 수정 성공 - 기본 정보 및 프로필 완성 로직")
    void updateMember_Success_WithProfileComplete() {
        Long memberId = 1L;
        Member member = Member.builder()
                .id(memberId)
                .name("Old")
                .email("old@test.com")
                .phone("01011112222")
                .birthDate(LocalDate.of(1000, 1, 1))
                .isProfileComplete(false)
                .grade(Grade.builder().gradeName("GENERAL").build())
                .status(Status.ACTIVE)
                .build();

        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .name("New")
                .phone("010-9999-8888")
                .email("new@test.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));


        given(sha256Utils.encrypt("new@test.com")).willReturn("newEmailHash");
        given(sha256Utils.encrypt("01099998888")).willReturn("newPhoneHash");

        given(memberRepository.existsByEmailHash("newEmailHash")).willReturn(false);
        given(memberRepository.existsByPhoneHash("newPhoneHash")).willReturn(false);

        MemberResponse response = memberService.updateMember(memberId, request);

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getPhone()).isEqualTo("01099998888");
        assertThat(member.isProfileComplete()).isTrue();
    }

    @Test
    @DisplayName("회원 수정 실패 - 존재하지 않는 회원")
    void updateMember_Fail_NotFound() {
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateMember(memberId, MemberUpdateRequest.builder().build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원 수정 - 본인 이메일/전화번호로 변경 시 중복체크 스킵")
    void updateMember_Skip_DuplicateCheck_IfSame() {
        Long memberId = 1L;
        String sameEmail = "same@test.com";
        String samePhone = "01012341234";

        Member member = Member.builder()
                .id(memberId)
                .email(sameEmail)
                .phone(samePhone)
                .grade(Grade.builder().gradeName("GENERAL").build())
                .status(Status.ACTIVE)
                .build();

        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .email(sameEmail)
                .phone(samePhone)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        memberService.updateMember(memberId, request);

        verify(memberRepository, never()).existsByEmailHash(anyString());
        verify(memberRepository, never()).existsByPhoneHash(anyString());
    }

    @Test
    @DisplayName("회원 수정 실패 - 이메일 중복")
    void updateMember_Fail_DuplicateEmail() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).email("old@test.com").build();
        MemberUpdateRequest request = MemberUpdateRequest.builder().email("new@test.com").build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        given(sha256Utils.encrypt("new@test.com")).willReturn("newHash");
        given(memberRepository.existsByEmailHash("newHash")).willReturn(true);

        assertThatThrownBy(() -> memberService.updateMember(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("회원 수정 실패 - 전화번호 중복")
    void updateMember_Fail_DuplicatePhone() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).phone("01000000000").build();
        MemberUpdateRequest request = MemberUpdateRequest.builder().phone("010-9999-9999").build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        given(sha256Utils.encrypt("01099999999")).willReturn("newPhoneHash");
        given(memberRepository.existsByPhoneHash("newPhoneHash")).willReturn(true);

        assertThatThrownBy(() -> memberService.updateMember(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PHONE);
    }

    @Test
    @DisplayName("회원 수정 실패 - 생년월일 변경 불가")
    void updateMember_Fail_BirthDateChange() {
        Long memberId = 1L;
        Member member = Member.builder()
                .id(memberId)
                .birthDate(LocalDate.of(2000, 1, 1))
                .isProfileComplete(true)
                .build();
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateMember(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BIRTHDATE_CANNOT_CHANGE);
    }


    @Test
    @DisplayName("생일자 조회 성공")
    void getBirthdayMemberIds_Success() {
        List<Long> ids = List.of(1L, 2L);
        given(memberRepository.findAllIdsByBirthMonth(1)).willReturn(ids);

        List<Long> result = memberService.getBirthdayMemberIds(1);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("생일자 조회 실패 - 잘못된 월 (13월)")
    void getBirthdayMemberIds_Fail_InvalidMonth_Over() {
        assertThatThrownBy(() -> memberService.getBirthdayMemberIds(13))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("생일자 조회 실패 - 잘못된 월 (0월)")
    void getBirthdayMemberIds_Fail_InvalidMonth_Under() {
        assertThatThrownBy(() -> memberService.getBirthdayMemberIds(0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }


    @Test
    @DisplayName("권한 수정 성공")
    void updateRole_Success() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).role(Role.USER).build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberService.updateRole(memberId, Role.ADMIN);

        assertThat(member.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("권한 수정 실패 - 존재하지 않는 회원")
    void updateRole_Fail_NotFound() {
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateRole(memberId, Role.ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }


    @Test
    @DisplayName("간편 회원 정보 조회 성공")
    void getMembersInfo_Success() {
        List<Long> ids = List.of(1L);
        Object[] row = {1L, "loginId", "name"};

        given(memberRepository.findSimpleMembers(ids)).willReturn(Collections.singletonList(row));

        List<SimpleMemberResponse> result = memberService.getMembersInfo(ids);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("name");
    }

    @Test
    @DisplayName("간편 회원 정보 조회 - 빈 리스트 입력")
    void getMembersInfo_Empty() {
        List<SimpleMemberResponse> result = memberService.getMembersInfo(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("간편 회원 정보 조회 - Null 입력")
    void getMembersInfo_Null() {
        List<SimpleMemberResponse> result = memberService.getMembersInfo(null);
        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("휴면 회원 확인 성공")
    void checkDormantMember_Success() {
        Member member = Member.builder().status(Status.DORMANT).build();
        String hash = "hash";

        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.of(member));

        memberService.checkDormantMember("test", "t@t.com");
    }

    @Test
    @DisplayName("휴면 회원 확인 실패 - 회원이 아님")
    void checkDormantMember_Fail_NotFound() {
        String hash = "hash";
        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.checkDormantMember("test", "t@t.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("휴면 회원 확인 실패 - 휴면 상태가 아님")
    void checkDormantMember_Fail_NotDormant() {
        Member member = Member.builder().status(Status.ACTIVE).build();
        String hash = "hash";

        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.checkDormantMember("test", "t@t.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_DORMANT);
    }


    @Test
    @DisplayName("휴면 해제 성공")
    void activateDormantMember_Success() {
        Member member = Member.builder().status(Status.DORMANT).build();
        String hash = "hash";

        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.of(member));

        memberService.activateDormantMember("test", "t@t.com");

        assertThat(member.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(member.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("휴면 해제 실패 - 회원 없음")
    void activateDormantMember_Fail_NotFound() {
        String hash = "hash";
        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.activateDormantMember("test", "t@t.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("휴면 해제 실패 - 탈퇴한 회원")
    void activateDormantMember_Fail_Withdrawn() {
        Member member = Member.builder().status(Status.WITHDRAWAL).build();
        String hash = "hash";

        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.activateDormantMember("test", "t@t.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_WITHDRAWN);
    }

    @Test
    @DisplayName("휴면 해제 - 이미 활성 회원은 로직 건너뜀")
    void activateDormantMember_AlreadyActive() {
        Member member = Member.builder().status(Status.ACTIVE).build();
        String hash = "hash";

        given(sha256Utils.encrypt("t@t.com")).willReturn(hash);
        given(memberRepository.findByLoginIdAndEmailHash("test", hash)).willReturn(Optional.of(member));

        memberService.activateDormantMember("test", "t@t.com");

        assertThat(member.getStatus()).isEqualTo(Status.ACTIVE);
    }
}