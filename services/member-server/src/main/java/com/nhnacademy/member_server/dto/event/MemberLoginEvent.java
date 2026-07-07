package com.nhnacademy.member_server.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberLoginEvent {
    private Long memberId;
}