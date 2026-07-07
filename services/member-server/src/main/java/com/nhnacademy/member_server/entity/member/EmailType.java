package com.nhnacademy.member_server.entity.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailType {
    SIGNUP("EMAIL_CHECK:", true, false),

    RESET_PASSWORD("PW_EMAIL_CHECK:", false, true),

    FIND_ID("ID_EMAIL_CHECK:", false, true),

    ACTIVATE("ACTIVATE:", false, true);

    private final String prefix;
    private final boolean checkDuplication;
    private final boolean checkExistence;
}