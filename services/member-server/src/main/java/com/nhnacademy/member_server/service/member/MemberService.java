package com.nhnacademy.member_server.service.member;

import com.nhnacademy.member_server.dto.request.member.MemberUpdateRequest;
import com.nhnacademy.member_server.dto.response.member.MemberResponse;
import com.nhnacademy.member_server.dto.response.member.SimpleMemberResponse;
import com.nhnacademy.member_server.entity.member.Role;
import java.util.List;

public interface MemberService {
    void withdraw(Long userId);

    MemberResponse getMember(Long memberId);

    MemberResponse updateMember(Long memberId, MemberUpdateRequest memberUpdateRequest);

    List<Long> getBirthdayMemberIds(int month);

    void updateRole(Long memberId, Role newRole);

    List<SimpleMemberResponse> getMembersInfo(List<Long> memberIds);

    void checkDormantMember(String loginId, String email);

    void activateDormantMember(String loginId, String email);
}
