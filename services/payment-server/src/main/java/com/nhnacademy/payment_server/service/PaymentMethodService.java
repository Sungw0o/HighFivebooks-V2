package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import java.util.List;

public interface PaymentMethodService {
    List<PaymentMethodResponse> getAllMethods();
    void updateStatus(Long methodId, boolean isActive);
}
