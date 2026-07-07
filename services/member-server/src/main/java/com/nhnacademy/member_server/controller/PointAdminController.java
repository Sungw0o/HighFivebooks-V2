package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.PointAdminApi;
import com.nhnacademy.member_server.dto.request.point.PointAdminAdjustmentRequest;
import com.nhnacademy.member_server.dto.request.point.PointAdminPolicyRequest;
import com.nhnacademy.member_server.dto.response.point.PointAdminPolicyResponse;
import com.nhnacademy.member_server.dto.response.point.PointTransactionResponse;
import com.nhnacademy.member_server.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class PointAdminController implements PointAdminApi {

    private final PointService pointService;

    @Override
    @GetMapping("/policy")
    public ResponseEntity<PointAdminPolicyResponse> getPolicy() {

        return ResponseEntity.ok(pointService.getRecentPolicy());
    }

    @Override
    @PostMapping("/policy")
    public ResponseEntity<Void> updatePolicy(@RequestBody PointAdminPolicyRequest request) {
        pointService.updatePolicy(request);

        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/adjustment")
    public ResponseEntity<PointTransactionResponse> adjustmentMemberPoint(@RequestBody PointAdminAdjustmentRequest request) {
        Long currentPoint = pointService.adjustmentMemberPoint(request);

        return ResponseEntity.ok(new PointTransactionResponse(request.getMemberId(), currentPoint));
    }
}
