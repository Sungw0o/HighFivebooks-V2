package com.nhnacademy.member_server.repository;

import com.nhnacademy.member_server.entity.member.Grade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByGradeName(String gradeName);
}
