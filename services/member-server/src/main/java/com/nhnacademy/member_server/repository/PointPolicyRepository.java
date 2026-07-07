package com.nhnacademy.member_server.repository;

import com.nhnacademy.member_server.entity.point.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {
    PointPolicy findTopByOrderByUpdatedAtDesc(); // 수정일 기준 내림차순 정렬해서 맨위 1개 가져오기
}
