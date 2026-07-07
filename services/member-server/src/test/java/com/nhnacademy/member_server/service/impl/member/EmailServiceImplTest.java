package com.nhnacademy.member_server.service.impl.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.utils.Sha256Utils;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @InjectMocks
    EmailServiceImpl emailService;

    @Mock
    JavaMailSender mailSender;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    MemberRepository memberRepository;
    @Mock
    ValueOperations<String, String> valueOperations;
    @Mock
    Sha256Utils sha256Utils;

    @Test
    @DisplayName("회원가입 인증메일 발송 - 중복된 이메일 실패")
    void sendSignupCodeDuplicateFailTest() {
        String email = "exist@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(true);

        assertThatThrownBy(() -> emailService.sendVerificationCode(email, EmailType.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증메일 발송 - 존재하지 않는 이메일 실패")
    void sendResetPasswordCodeNotFoundFailTest() {
        String email = "notfound@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(false);

        assertThatThrownBy(() -> emailService.sendVerificationCode(email, EmailType.RESET_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("인증메일 발송 성공 (SIGNUP)")
    void sendVerificationCodeSuccessTest() {
        String email = "new@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(false);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        emailService.sendVerificationCode(email, EmailType.SIGNUP);

        String key = EmailType.SIGNUP.getPrefix() + email;
        verify(valueOperations).set(eq(key), any(String.class), any(Duration.class));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("인증코드 검증 성공")
    void verifyCodeSuccessTest() {
        String email = "test@test.com";
        String code = "123456";
        String key = EmailType.SIGNUP.getPrefix() + email;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(key)).willReturn(code);

        boolean result = emailService.verifyCode(email, code, EmailType.SIGNUP);

        assertThat(result).isTrue();
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 코드 불일치")
    void verifyCodeMismatchTest() {
        String email = "test@test.com";
        String key = EmailType.SIGNUP.getPrefix() + email;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(key)).willReturn("123456");

        boolean result = emailService.verifyCode(email, "000000", EmailType.SIGNUP);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 만료된 코드(Redis에 없음)")
    void verifyCodeExpiredTest() {
        String email = "test@test.com";
        String key = EmailType.SIGNUP.getPrefix() + email;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(key)).willReturn(null);

        boolean result = emailService.verifyCode(email, "123456", EmailType.SIGNUP);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증코드 검증 실패 - 입력값이 Null인 경우")
    void verifyCode_NullInput_Test() {
        assertThat(emailService.verifyCode(null, "123456", EmailType.SIGNUP)).isFalse();
        assertThat(emailService.verifyCode("email", null, EmailType.SIGNUP)).isFalse();
        assertThat(emailService.verifyCode(null, null, EmailType.SIGNUP)).isFalse();
    }

    @Test
    @DisplayName("메일 발송 실패 - JavaMailSender 예외 발생")
    void sendVerificationCode_MailSendException_Test() {
        String email = "new@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(false);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        doThrow(new MailSendException("Mail Error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendVerificationCode(email, EmailType.SIGNUP))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAIL_SEND_ERROR);
    }

    @Test
    @DisplayName("인증메일 발송 - FIND_ID 타입 (메일 제목/내용 확인)")
    void sendVerificationCode_FindId_Test() {
        String email = "exist@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        emailService.sendVerificationCode(email, EmailType.FIND_ID);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getSubject()).contains("아이디 찾기");
        assertThat(sentMessage.getText()).contains("아이디 찾기를 위한 인증 번호");
    }

    @Test
    @DisplayName("인증메일 발송 - ACTIVATE 타입 (메일 제목/내용 확인)")
    void sendVerificationCode_Activate_Test() {
        String email = "exist@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        emailService.sendVerificationCode(email, EmailType.ACTIVATE);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getSubject()).contains("휴면 계정 활성화");
        assertThat(sentMessage.getText()).contains("휴면 해제를 위한 인증 번호");
    }

    @Test
    @DisplayName("인증메일 발송 - RESET_PASSWORD 타입 (메일 제목/내용 확인)")
    void sendVerificationCode_ResetPassword_Test() {
        String email = "exist@test.com";
        String hash = "hashed_email";

        given(sha256Utils.encrypt(email)).willReturn(hash);
        given(memberRepository.existsByEmailHash(hash)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        emailService.sendVerificationCode(email, EmailType.RESET_PASSWORD);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getSubject()).contains("비밀번호 재설정");
        assertThat(sentMessage.getText()).contains("비밀번호 재설정을 위한 인증 번호");
    }
}