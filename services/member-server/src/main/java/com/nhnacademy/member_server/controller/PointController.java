package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.PointUserApi;
import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointHistoryResponse;
import com.nhnacademy.member_server.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController implements PointUserApi {

    private final PointService pointService;

    @Override
    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponse> getMyBalance(@RequestHeader("X-USER-ID") Long memberId){

        PointBalanceResponse responseDto = pointService.getBalance(memberId);

        return ResponseEntity.ok(responseDto);
    }

    @Override
    @GetMapping("/history")
    public ResponseEntity<Page<PointHistoryResponse>> getMyHistory(@RequestHeader("X-USER-ID") Long memberId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size){

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending()); // id 기준 최신순 정렬

        Page<PointHistoryResponse> history = pointService.getHistory(memberId, pageable);

        return ResponseEntity.ok(history);
    }
}