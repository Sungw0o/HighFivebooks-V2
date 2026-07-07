package com.nhnacademy.member_server.service.impl.member;

import com.nhnacademy.member_server.dto.event.MemberLoginEvent;
import com.nhnacademy.member_server.dto.event.MemberLogoutEvent;
import com.nhnacademy.member_server.dto.message.CouponIssueMessage;
import com.nhnacademy.member_server.dto.request.member.MemberCreateRequest;
import com.nhnacademy.member_server.dto.request.member.PasswordResetRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.dto.response.member.TokenDto;
import com.nhnacademy.member_server.dto.response.social.OAuth2UserInfo;
import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.entity.member.Gender;
import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.entity.point.PointEventType;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.global.jwt.JwtUtil;
import com.nhnacademy.member_server.repository.GradeRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.security.UserDetailsImpl;
import com.nhnacademy.member_server.service.PointService;
import com.nhnacademy.member_server.service.member.AuthService;
import com.nhnacademy.member_server.service.member.EmailService;
import com.nhnacademy.member_server.service.social.SocialLoginFactory;
import com.nhnacademy.member_server.service.social.SocialLoginStrategy;
import com.nhnacademy.member_server.utils.Sha256Utils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final GradeRepository gradeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SocialLoginFactory socialLoginFactory;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final PointService pointService;

    private final Sha256Utils sha256Utils;

    @Value("${jwt.refresh_expiration_time}")
    private Long refreshExpirationTime;
    private static final String GENERAL_GRADE = "GENERAL";

    @Override
    @Transactional
    public TokenDto loginUser(String loginId, String password) {
        Member dbMember = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (dbMember.getStatus() == Status.DORMANT) {throw new BusinessException(ErrorCode.MEMBER_DORMANT);
        }
        if (dbMember.getStatus() == Status.WITHDRAWAL) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (!passwordEncoder.matches(password, dbMember.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginId, password)
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Member inputMember =  userDetails.getMember();

        dbMember.setLastLoginAt(LocalDateTime.now());

        Long memberId = inputMember.getId();
        Role role = inputMember.getRole();

        boolean isProfileComplete = dbMember.isProfileComplete();

        String accessToken = jwtUtil.createAccessToken(memberId, role);
        String refreshToken = jwtUtil.createRefreshToken(memberId);

        redisTemplate.opsForValue().set(
                "RT:" + memberId,
                refreshToken,
                refreshExpirationTime,
                TimeUnit.MILLISECONDS
        );

        eventPublisher.publishEvent(new MemberLoginEvent(dbMember.getId()));

        return new TokenDto(accessToken, refreshToken, isProfileComplete);
    }

    @Override
    @Transactional
    public void signup(MemberCreateRequest request) {
        String safeLoginId = request.getLoginId().trim();
        String safeName = request.getName().trim();
        String safeEmail = request.getEmail().trim();
        String rawPhone = request.getPhone().replaceAll("[^0-9]", "");

        String emailHash = sha256Utils.encrypt(safeEmail);
        String phoneHash = sha256Utils.encrypt(rawPhone);

        if (memberRepository.existsByLoginId(safeLoginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        else if (memberRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        else if (memberRepository.existsByPhoneHash(phoneHash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        Grade basicGrade = gradeRepository.findByGradeName(GENERAL_GRADE)
                .orElseGet(() -> gradeRepository.save(Grade.builder()
                        .gradeName(GENERAL_GRADE)
                        .min(0)
                        .pointRate(new BigDecimal("0.01"))
                        .max(100000)
                        .build()
                ));

        Role finalRole = Role.USER;

        Member member = Member.builder()
                .loginId(safeLoginId)
                .password(passwordEncoder.encode(request.getPassword()))
                .name(safeName)
                .gender(request.getGender())
                .phone(rawPhone)
                .phoneHash(phoneHash)
                .email(safeEmail)
                .emailHash(emailHash)
                .birthDate(request.getBirthDate())
                .lastLoginAt(LocalDateTime.now())
                .status(Status.ACTIVE)
                .role(finalRole)
                .currentPoint(0L)
                .grade(basicGrade)
                .isProfileComplete(true)
                .build();

        Member savedMember = memberRepository.save(member);
        processSignupPoint(savedMember);
        processWelcomeCoupon(savedMember);
    }

    @Override
    @Transactional
    public TokenDto reissue(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtUtil.getUserId(refreshToken);
        String redisToken = redisTemplate.opsForValue().get("RT:" + memberId);

        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getStatus().equals(Status.WITHDRAWAL)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        boolean isProfileComplete = member.isProfileComplete();

        String newAccessToken = jwtUtil.createAccessToken(member.getId(), member.getRole());
        String newRefreshToken = jwtUtil.createRefreshToken(member.getId());

        redisTemplate.opsForValue().set(
                "RT:" + memberId,
                newRefreshToken,
                refreshExpirationTime,
                TimeUnit.MILLISECONDS
        );

        return new TokenDto(newAccessToken, newRefreshToken, isProfileComplete);
    }

    @Override
    public void logout(String accessToken, Long memberId) {
        redisTemplate.delete("RT:" + memberId);
        long expiration = jwtUtil.getRemainingTime(accessToken);
        if (expiration > 0) {
            redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
        }
        eventPublisher.publishEvent(new MemberLogoutEvent(memberId));
    }

    @Override
    @Transactional
    public TokenDto loginSocial(String provider, String code) {
        SocialLoginStrategy strategy = socialLoginFactory.getStrategy(provider);
        OAuth2UserInfo userInfo = strategy.getUserInfo(code);
        String providerId = userInfo.getProviderId();

        Member member = memberRepository.findByProviderId(providerId).orElse(null);

        if (member != null) {
            if (member.getStatus() == com.nhnacademy.member_server.entity.member.Status.DORMANT) {
                throw new BusinessException(ErrorCode.MEMBER_DORMANT);
            }

            if (member.getStatus() == com.nhnacademy.member_server.entity.member.Status.WITHDRAWAL) {
                throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
            }
            member.setLastLoginAt(java.time.LocalDateTime.now());
        } else {
            log.info("소셜 신규 회원 감지. 자동 가입 진행: {} / {}", provider, userInfo.getName());
            member = socialSignup(userInfo);
        }
        boolean isProfileComplete = member.isProfileComplete();

        String accessToken = jwtUtil.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtUtil.createRefreshToken(member.getId());

        redisTemplate.opsForValue().set(
                "RT:" + member.getId(),
                refreshToken,
                refreshExpirationTime,
                TimeUnit.MILLISECONDS
        );

        return new TokenDto(accessToken, refreshToken, isProfileComplete);
    }

    private Member socialSignup(OAuth2UserInfo userInfo) {
        String provider = userInfo.getProvider();
        String providerId = userInfo.getProviderId();

        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        String uniqueLoginId = provider + "_" + providerId;

        Grade basicGrade = gradeRepository.findByGradeName(GENERAL_GRADE)
                .orElseGet(() -> gradeRepository.save(
                        Grade.builder().gradeName(GENERAL_GRADE).min(0).pointRate(new BigDecimal("0.01")).build()
                ));

        String realName = (userInfo.getName() != null) ? userInfo.getName() : provider + " User";
        String realEmail = (userInfo.getEmail() != null) ? userInfo.getEmail() : uniqueLoginId + "@no-email.com";
        String realPhone = (userInfo.getMobile() != null) ? userInfo.getMobile() : "01000000000";

        if (realPhone.startsWith("82")) {
            realPhone = "010" + realPhone.substring(4);
        }

        String emailHash = sha256Utils.encrypt(realEmail);
        String phoneHash = sha256Utils.encrypt(realPhone);

        Gender gender = Gender.UNKNOWN;
        if ("MALE".equals(userInfo.getGender())) gender = Gender.MALE;
        else if ("FEMALE".equals(userInfo.getGender())) gender = Gender.FEMALE;

        java.time.LocalDate birthDate = java.time.LocalDate.of(1000, 1, 1);
        String rawBirth = userInfo.getBirthday();
        if (rawBirth != null) {
            try {
                if (rawBirth.length() == 8) {
                    birthDate = java.time.LocalDate.parse(rawBirth, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                }
                else if (rawBirth.length() == 4) {
                    String fullBirth = "0000" + rawBirth;
                    birthDate = java.time.LocalDate.parse(fullBirth, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                }
            } catch (Exception e) {
                log.warn("생일 파싱 실패 (기본값 사용): {}", rawBirth);
            }
        }

        Member member = Member.builder()
                .loginId(uniqueLoginId)
                .password(randomPassword)
                .name(realName)
                .email(realEmail)
                .emailHash(emailHash)
                .phone(realPhone)
                .phoneHash(phoneHash)
                .birthDate(birthDate)
                .gender(gender)
                .status(Status.ACTIVE)
                .role(Role.USER)
                .currentPoint(0L)
                .grade(basicGrade)
                .lastLoginAt(java.time.LocalDateTime.now())
                .provider(provider)
                .providerId(providerId)
                .isProfileComplete(false)
                .build();

        Member savedMember = memberRepository.save(member);
        processSignupPoint(savedMember);
        processWelcomeCoupon(savedMember);

        return savedMember;
    }


    @Override
    public String findLoginIdByEmail(String email, String code) {
        boolean isVerified = emailService.verifyCode(email, code, EmailType.FIND_ID);

        if (!isVerified) {
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        String emailHash = sha256Utils.encrypt(email);
        Member member = memberRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return maskLoginId(member.getLoginId());
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String loginId = request.getLoginId();
        String email = request.getEmail();
        String authCode = request.getAuthCode();
        String newPassword = request.getNewPassword();

        boolean isVerified = emailService.verifyCode(email, authCode, EmailType.RESET_PASSWORD);
        if (!isVerified) {
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        String emailHash = sha256Utils.encrypt(request.getEmail());
        Member member = memberRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (loginId != null && !loginId.isBlank() && !member.getLoginId().equals(loginId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    private String maskLoginId(String loginId) {
        if (loginId == null) {
            return null;
        }

        int len = loginId.length();

        if (len < 2) {
            return loginId;
        }

        if (len == 2) {
            return loginId.substring(0, 1) + "*";
        }

        return loginId.substring(0, 2) + "*".repeat(len - 2);
    }

    private void processSignupPoint(Member member) {
        try {
            pointService.createTransaction(PointTransactionCreateRequest.builder()
                    .memberId(member.getId())
                    .pointEventType(PointEventType.EARN_SIGNUP)
                    .build());
            log.info("신규 회원({}) 회원가입 포인트 적립 완료", member.getId());
        } catch (Exception e) {
            log.error("회원가입 포인트 적립 실패 (memberId={}): {}", member.getId(), e.getMessage());
        }
    }

    private void processWelcomeCoupon(Member member) {
        try {
            CouponIssueMessage message = new CouponIssueMessage(member.getId());
            rabbitTemplate.convertAndSend("high-five-coupon-welcome-queue", message);
            log.info("신규 회원({}) 웰컴 쿠폰 지급 메시지 발행 완료", member.getId());
        } catch (Exception e) {
            log.error("웰컴 쿠폰 메시지 발행 실패 (memberId={}): {}", member.getId(), e.getMessage());
        }
    }
}