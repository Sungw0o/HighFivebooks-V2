package com.nhnacademy.member_server.service.impl.member;

import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.member.EmailService;
import com.nhnacademy.member_server.utils.Sha256Utils;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Sha256Utils sha256Utils;

    @Override
    public void sendVerificationCode(String email, EmailType type) {
        String emailHash = sha256Utils.encrypt(email);
        boolean exists = memberRepository.existsByEmailHash(emailHash);

        if (type.isCheckDuplication() && exists) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (type.isCheckExistence() && !exists) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        String code = createRandomCode();
        String key = type.getPrefix() + email;

        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
        log.info("[{}] 인증번호 발송: email={}, key={}", type, email, key);

        sendMail(email, code, type);
    }

    @Override
    public boolean verifyCode(String email, String inputCode, EmailType type) {

        if (email == null || inputCode == null) {
            return false;
        }

        String key = type.getPrefix() + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(inputCode)) {
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    private void sendMail(String email, String code, EmailType type) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);

        if (type == EmailType.SIGNUP) {
            message.setSubject("[HighFive] 회원가입 인증번호");
            message.setText("회원가입을 위한 인증 번호는 [" + code + "] 입니다.\n5분 내에 입력해 주세요.");
        } else if (type == EmailType.RESET_PASSWORD) {
            message.setSubject("[HighFive] 비밀번호 재설정 인증번호");
            message.setText("비밀번호 재설정을 위한 인증 번호는 [" + code + "] 입니다.\n타인에게 노출되지 않도록 주의하세요.");
        } else if (type == EmailType.FIND_ID) {
            message.setSubject("[HighFive] 아이디 찾기 인증번호");
            message.setText("아이디 찾기를 위한 인증 번호는 [" + code + "] 입니다.\n타인에게 노출되지 않도록 주의하세요.");
        }
        else if (type == EmailType.ACTIVATE) {
            message.setSubject("[HighFive] 휴면 계정 활성화 인증번호");
            message.setText("휴면 해제를 위한 인증 번호는 [" + code + "] 입니다.\n5분 내에 입력해 주세요.");
        }

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("메일 발송 실패: {}", email, e);
            throw new BusinessException(ErrorCode.MAIL_SEND_ERROR);
        }
    }

    private String createRandomCode() {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            key.append(SECURE_RANDOM.nextInt(10));
        }
        return key.toString();
    }
}