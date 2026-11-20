package com.nhnacademy.member_server.dto.response.member;

import com.nhnacademy.member_server.entity.member.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberGradeResponse {

    private final Double earnRate;

    public static MemberGradeResponse from(Member member) {
        return new MemberGradeResponse(member.getGrade().getPointRate().doubleValue());
    }
}
