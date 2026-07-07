package com.nhnacademy.member_server.dto.response.social;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaycoMemberResponse {
    
    private PaycoHeader header;
    private PaycoData data;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaycoHeader {
        @JsonProperty("isSuccessful")
        private boolean isSuccessful;
        private int resultCode;
        private String resultMessage;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaycoData {
        private PaycoMember member;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaycoMember {
        private String idNo;
        private String name;
        private String email;
        private String mobile;
        private String genderCode;
        private String birthdayMMdd;
        private String birthday;
    }
}