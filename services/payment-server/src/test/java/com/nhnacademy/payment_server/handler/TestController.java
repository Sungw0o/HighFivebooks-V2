package com.nhnacademy.payment_server.handler;

import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/business-error")
    public void throwBusinessException() {
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
