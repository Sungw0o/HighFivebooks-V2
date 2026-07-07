package com.nhnacademy.member_server.utils;

import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class Sha256Utils {
    public String encrypt(String text) {
        if (text == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SHA256_ALGORITHM_NOT_FOUND);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SHA256_ENCRYPTION_FAILED);
        }
    }
}