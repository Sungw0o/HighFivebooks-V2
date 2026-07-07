package com.nhnacademy.payment_server.service.impl;

import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.service.PaymentMethodService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public List<PaymentMethodResponse> getAllMethods() {
        return paymentMethodRepository.findAll().stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(Long methodId, boolean isActive) {
        PaymentMethod method = paymentMethodRepository.findById(methodId).orElseThrow(()
                -> new BusinessException(ErrorCode.METHOD_NOT_FOUND));
        method.updateStatus(isActive);
    }
}