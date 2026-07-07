package com.nhnacademy.member_server.dto.response.member;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nhnacademy.member_server.entity.member.Member;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private String name;

    @Email
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String phone;
    private String status;

    private String gradeName;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .birthDate(member.getBirthDate())
                .status(member.getStatus().name())
                .gradeName(member.getGrade().getGradeName())
                .build();
    }
}
