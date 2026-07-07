package com.nhnacademy.member_server.service;

import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.repository.GradeRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeInitService {

    private final GradeRepository gradeRepository;

    @Transactional
    public void createGradeIfNotExists(String name, int min, Integer max, String rate) {
        if (gradeRepository.findByGradeName(name).isPresent()) {
            return;
        }

        gradeRepository.save(Grade.builder()
                .gradeName(name)
                .min(min)
                .max(max)
                .pointRate(new BigDecimal(rate))
                .build());
    }
}
