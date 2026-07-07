package com.nhnacademy.member_server.handler;

import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e){
        log.warn("BusinessException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e){
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("C001", errorMessage));
    }

    // 나머지 알 수 없는 에러들 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("알수 없는 Exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("C002", "알 수 없는 서버 오류가 발생했습니다."));
    }

    // FeignException 처리 (응답은 줬지만 에러를 준 경우)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        log.error("Feign Error: status={}, message={}", e.status(), e.getMessage());

        // 책이 없을 경우
        if (e.status() == 404) {
            ErrorCode errorCode = ErrorCode.BOOK_NOT_FOUND_IN_SERVER;
            return ResponseEntity
                    .status(errorCode.getStatus())
                    .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
        }

        // 그 외 나머지 에러들
        ErrorCode errorCode = ErrorCode.EXTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), "Book Service 오류: " + e.getMessage()));
    }

    // 상대 book 서버가 아예 꺼져있을 경우
    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ErrorResponse> handleRetryableException(RetryableException e) {
        log.error("Feign Connection Error: {}", e.getMessage());

        ErrorCode errorCode = ErrorCode.EXTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(errorCode.getCode(), "현재 도서 서비스를 이용할 수 없습니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("C409", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("C400", e.getMessage()));
    }

    public record ErrorResponse(String code, String message){}
}
