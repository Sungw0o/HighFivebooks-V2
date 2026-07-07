package com.nhnacademy.member_server.entity.point;

public enum PointStatus {
    RESERVED,   // 사용 예약 (차감 대기)
    CONFIRMED,  // 사용 확정
    CANCELED    // 취소 (환불됨)
}
