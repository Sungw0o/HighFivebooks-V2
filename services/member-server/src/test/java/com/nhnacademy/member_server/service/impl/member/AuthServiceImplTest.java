package com.nhnacademy.member_server.service.impl.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhnacademy.member_server.dto.event.MemberLoginEvent;
import com.nhnacademy.member_server.dto.event.MemberLogoutEvent;
import com.nhnacademy.member_server.dto.message.CouponIssueMessage;
import com.nhnacademy.member_server.dto.request.member.MemberCreateRequest;
import com.nhnacademy.member_server.dto.request.member.PasswordResetRequest;
import com.nhnacademy.member_server.dto.response.member.TokenDto;
import com.nhnacademy.member_server.dto.response.social.OAuth2UserInfo;
import com.nhnacademy.member_server.entity.member.Gender;
import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.global.jwt.JwtUtil;
import com.nhnacademy.member_server.repository.GradeRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.security.UserDetailsImpl;
import com.nhnacademy.member_server.service.member.EmailService;
import com.nhnacademy.member_server.service.social.SocialLoginFactory;
import com.nhnacademy.member_server.service.social.SocialLoginStrategy;
import com.nhnacademy.member_server.utils.Sha256Utils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private GradeRepository gradeRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private SocialLoginFactory socialLoginFactory;
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Sha256Utils sha256Utils;

    private Member member;
    private Grade grade;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(authService, "refreshExpirationTime", 3600000L);

        grade = Grade.builder().gradeName("GENERAL").pointRate(BigDecimal.valueOf(0.01)).build();
        member = Member.builder()
                .id(1L)
                .loginId("testUser")
                .password("encodedPw")
                .email("test@test.com")
                .phone("01012345678")
                .status(Status.ACTIVE)
                .role(Role.USER)
                .grade(grade)
                .isProfileComplete(true)
                .build();
    }

    @Test
    void loginUser_Success() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(member);

        when(memberRepository.findByLoginId(anyString())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.createAccessToken(anyLong(), any())).thenReturn("access");
        when(jwtUtil.createRefreshToken(anyLong())).thenReturn("refresh");

        TokenDto result = authService.loginUser("testUser", "pw");

        assertThat(result.getAccessToken()).isEqualTo("access");
        verify(eventPublisher).publishEvent(any(MemberLoginEvent.class));
    }

    @Test
    void loginUser_NotFound() {
        when(memberRepository.findByLoginId(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginUser("test", "pw"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void loginUser_Dormant() {
        Member dormantMember = Member.builder().status(Status.DORMANT).build();
        when(memberRepository.findByLoginId(anyString())).thenReturn(Optional.of(dormantMember));

        assertThatThrownBy(() -> authService.loginUser("test", "pw"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_DORMANT);
    }

    @Test
    void loginUser_Withdrawn() {
        Member withdrawnMember = Member.builder().status(Status.WITHDRAWAL).build();
        when(memberRepository.findByLoginId(anyString())).thenReturn(Optional.of(withdrawnMember));

        assertThatThrownBy(() -> authService.loginUser("test", "pw"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
    }

    @Test
    void loginUser_PasswordMismatch() {
        when(memberRepository.findByLoginId(anyString())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.loginUser("test", "wrongPw"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void signup_Success() {
        MemberCreateRequest request = MemberCreateRequest.builder()
                .loginId("new").password("pw").name("nm").phone("010").email("e@e.com").gender(Gender.MALE).birthDate(LocalDate.now()).build();

        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.existsByEmailHash(any())).thenReturn(false);
        when(memberRepository.existsByPhoneHash(any())).thenReturn(false);
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);

        authService.signup(request);

        verify(rabbitTemplate).convertAndSend(eq("high-five-coupon-welcome-queue"), any(CouponIssueMessage.class));
    }

    @Test
    void signup_GradeNotFound_CreateGrade() {
        MemberCreateRequest request = MemberCreateRequest.builder()
                .loginId("new").password("pw").name("nm").phone("010").email("e@e.com").gender(Gender.MALE).birthDate(LocalDate.now()).build();

        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.existsByEmailHash(any())).thenReturn(false);
        when(memberRepository.existsByPhoneHash(any())).thenReturn(false);
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.empty());
        when(gradeRepository.save(any())).thenReturn(grade);
        when(memberRepository.save(any())).thenReturn(member);

        authService.signup(request);

        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    void signup_RabbitMQException() {
        MemberCreateRequest request = MemberCreateRequest.builder()
                .loginId("new").password("pw").name("nm").phone("010").email("e@e.com").gender(Gender.MALE).birthDate(LocalDate.now()).build();

        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.existsByEmailHash(any())).thenReturn(false);
        when(memberRepository.existsByPhoneHash(any())).thenReturn(false);
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);
        doThrow(new RuntimeException()).when(rabbitTemplate).convertAndSend(anyString(), any(CouponIssueMessage.class));

        authService.signup(request);

        verify(memberRepository).save(any());
    }

    @Test
    void signup_DuplicateId() {
        MemberCreateRequest request = MemberCreateRequest.builder().loginId("dup").name("n").email("e").phone("p").build();
        when(memberRepository.existsByLoginId(any())).thenReturn(true);
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
    }

    @Test
    void signup_DuplicateEmail() {
        MemberCreateRequest request = MemberCreateRequest.builder().loginId("new").email("dup").name("n").phone("p").build();
        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.existsByEmailHash(any())).thenReturn(true);
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void signup_DuplicatePhone() {
        MemberCreateRequest request = MemberCreateRequest.builder().loginId("new").email("new").phone("dup").name("n").build();
        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.existsByEmailHash(any())).thenReturn(false);
        when(memberRepository.existsByPhoneHash(any())).thenReturn(true);
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_PHONE);
    }

    @Test
    void reissue_Success() {
        String token = "rt";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(valueOperations.get("RT:1")).thenReturn(token);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtUtil.createAccessToken(any(), any())).thenReturn("at");
        when(jwtUtil.createRefreshToken(any())).thenReturn("rt");

        TokenDto result = authService.reissue(token);
        assertThat(result.getAccessToken()).isEqualTo("at");
    }

    @Test
    void reissue_InvalidToken() {
        when(jwtUtil.validateToken(any())).thenReturn(false);
        assertThatThrownBy(() -> authService.reissue("bad"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void reissue_TokenMismatch() {
        String token = "rt";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(valueOperations.get("RT:1")).thenReturn("other");
        assertThatThrownBy(() -> authService.reissue(token))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void reissue_MemberNotFound() {
        String token = "rt";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(valueOperations.get("RT:1")).thenReturn(token);
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.reissue(token))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void reissue_MemberWithdrawn() {
        String token = "rt";
        Member withdrawn = Member.builder().status(Status.WITHDRAWAL).build();
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(valueOperations.get("RT:1")).thenReturn(token);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(withdrawn));
        assertThatThrownBy(() -> authService.reissue(token))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void logout_Success() {
        when(jwtUtil.getRemainingTime(any())).thenReturn(1000L);
        authService.logout("at", 1L);
        verify(redisTemplate).delete("RT:1");
        verify(valueOperations).set(any(), eq("logout"), anyLong(), any());
        verify(eventPublisher).publishEvent(any(MemberLogoutEvent.class));
    }

    @Test
    void logout_ExpiredToken() {
        when(jwtUtil.getRemainingTime(any())).thenReturn(0L);
        authService.logout("at", 1L);
        verify(redisTemplate).delete("RT:1");
        verify(valueOperations, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    void loginSocial_Existing_Success() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder().providerId("pid").build();
        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.of(member));
        when(jwtUtil.createAccessToken(any(), any())).thenReturn("at");
        when(jwtUtil.createRefreshToken(any())).thenReturn("rt");

        authService.loginSocial("payco", "code");
        verify(jwtUtil).createAccessToken(any(), any());
    }

    @Test
    void loginSocial_Existing_Dormant() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder().providerId("pid").build();
        Member dormant = Member.builder().status(Status.DORMANT).build();
        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.of(dormant));

        assertThatThrownBy(() -> authService.loginSocial("payco", "code"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_DORMANT);
    }

    @Test
    void loginSocial_Existing_Withdrawn() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder().providerId("pid").build();
        Member withdrawn = Member.builder().status(Status.WITHDRAWAL).build();
        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> authService.loginSocial("payco", "code"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
    }

    @Test
    void loginSocial_New_FullInfo() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .providerId("pid")
                .provider("PAYCO")
                .name("name")
                .email("e@e.com")
                .mobile("01012345678")
                .gender("MALE")
                .birthday("20000101")
                .build();

        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);

        authService.loginSocial("PAYCO", "code");
        verify(memberRepository).save(any());
    }

    @Test
    void loginSocial_New_PhonePrefix() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .providerId("pid").provider("PAYCO").mobile("821012345678").build();

        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);

        authService.loginSocial("PAYCO", "code");
        verify(memberRepository).save(any());
    }

    @Test
    void loginSocial_New_GenderFemale() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .providerId("pid").provider("PAYCO").gender("FEMALE").build();

        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);

        authService.loginSocial("PAYCO", "code");
        verify(memberRepository).save(any());
    }

    @Test
    void loginSocial_New_BirthdayLength4() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .providerId("pid").provider("PAYCO").birthday("0101").build();

        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);

        authService.loginSocial("PAYCO", "code");
        verify(memberRepository).save(any());
    }

    @Test
    void loginSocial_New_BirthdayParseError() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder()
                .providerId("pid").provider("PAYCO").birthday("invalid").build();

        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);

        authService.loginSocial("PAYCO", "code");
        verify(memberRepository).save(any());
    }

    @Test
    void loginSocial_New_RabbitMQError() {
        SocialLoginStrategy strategy = mock(SocialLoginStrategy.class);
        OAuth2UserInfo userInfo = OAuth2UserInfo.builder().providerId("pid").provider("PAYCO").build();

        when(socialLoginFactory.getStrategy(any())).thenReturn(strategy);
        when(strategy.getUserInfo(any())).thenReturn(userInfo);
        when(memberRepository.findByProviderId("pid")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradeName("GENERAL")).thenReturn(Optional.of(grade));
        when(memberRepository.save(any())).thenReturn(member);
        doThrow(new RuntimeException()).when(rabbitTemplate).convertAndSend(anyString(), any(CouponIssueMessage.class));

        authService.loginSocial("PAYCO", "code");
        verify(memberRepository).save(any());
    }

    @Test
    void findLoginIdByEmail_VerifyFail() {
        when(emailService.verifyCode(any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> authService.findLoginIdByEmail("e", "c"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.AUTH_CODE_MISMATCH);
    }

    @Test
    void findLoginIdByEmail_MemberNotFound() {
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.findLoginIdByEmail("e", "c"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void findLoginIdByEmail_Success_Masking() {
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.of(member));
        String id = authService.findLoginIdByEmail("e", "c");
        assertThat(id).isEqualTo("te******");
    }

    @Test
    void findLoginIdByEmail_Success_MaskingShort() {
        Member shortMem = Member.builder().loginId("ab").build();
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.of(shortMem));
        String id = authService.findLoginIdByEmail("e", "c");
        assertThat(id).isEqualTo("a*");
    }

    @Test
    void findLoginIdByEmail_Success_MaskingTiny() {
        Member tinyMem = Member.builder().loginId("a").build();
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.of(tinyMem));
        String id = authService.findLoginIdByEmail("e", "c");
        assertThat(id).isEqualTo("a");
    }

    @Test
    void findLoginIdByEmail_Success_MaskingNull() {
        Member nullMem = Member.builder().loginId(null).build();
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.of(nullMem));
        String id = authService.findLoginIdByEmail("e", "c");
        assertThat(id).isNull();
    }

    @Test
    void resetPassword_VerifyFail() {
        PasswordResetRequest req = new PasswordResetRequest("id", "e", "c", "p");
        when(emailService.verifyCode(any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.AUTH_CODE_MISMATCH);
    }

    @Test
    void resetPassword_MemberNotFound() {
        PasswordResetRequest req = new PasswordResetRequest("id", "e", "c", "p");
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void resetPassword_IdMismatch() {
        PasswordResetRequest req = new PasswordResetRequest("diff", "e", "c", "p");
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.of(member));
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void resetPassword_Success() {
        PasswordResetRequest req = new PasswordResetRequest("testUser", "e", "c", "p");
        when(emailService.verifyCode(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByEmailHash(any())).thenReturn(Optional.of(member));
        authService.resetPassword(req);
        verify(memberRepository).save(member);
    }
}