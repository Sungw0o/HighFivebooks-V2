package com.nhnacademy.member_server.service.impl.member;

import com.nhnacademy.member_server.dto.request.member.MemberUpdateRequest;
import com.nhnacademy.member_server.dto.response.member.MemberResponse;
import com.nhnacademy.member_server.dto.response.member.SimpleMemberResponse;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.member.MemberService;
import com.nhnacademy.member_server.utils.Sha256Utils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final Sha256Utils sha256Utils;

    @Override
    @Transactional
    public void withdraw(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.setStatus(Status.WITHDRAWAL);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public MemberResponse updateMember(Long memberId, MemberUpdateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (req.getName() != null && !req.getName().isBlank()) {
            member.setName(req.getName());
        }

        if (req.getBirthDate() != null) {
            if (member.isProfileComplete() && !member.getBirthDate().equals(req.getBirthDate())) {
                throw new BusinessException(ErrorCode.BIRTHDATE_CANNOT_CHANGE);
            }
            member.setBirthDate(req.getBirthDate());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim();
            String newEmailHash = sha256Utils.encrypt(newEmail);

            if (!newEmail.equals(member.getEmail()) && memberRepository.existsByEmailHash(newEmailHash)) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            member.setEmail(newEmail);
            member.setEmailHash(newEmailHash);
        }

        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            String rawPhone = req.getPhone().replaceAll("[^0-9]", "");
            String newPhoneHash = sha256Utils.encrypt(rawPhone);

            if (!rawPhone.equals(member.getPhone()) && memberRepository.existsByPhoneHash(newPhoneHash)) {
                throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
            }
            member.setPhone(rawPhone);
            member.setPhoneHash(newPhoneHash);
        }

        if (req.getGender() != null) {
            member.setGender(req.getGender());
        }

        if (!member.isProfileComplete()) {
            java.time.LocalDate defaultDate = java.time.LocalDate.of(1000, 1, 1);
            if (member.getBirthDate() != null && !member.getBirthDate().equals(defaultDate)) {
                member.setProfileComplete(true);
            }
        }

        return MemberResponse.from(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBirthdayMemberIds(int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return memberRepository.findAllIdsByBirthMonth(month);
    }

    @Override
    @Transactional
    public void updateRole(Long memberId, Role newRole) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.setRole(newRole);
    }


    @Override
    @Transactional(readOnly = true)
    public List<SimpleMemberResponse> getMembersInfo(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> results = memberRepository.findSimpleMembers(memberIds);

        return results.stream()
                .map(row -> SimpleMemberResponse.builder()
                        .memberId((Long) row[0])
                        .name((String) row[2])
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void checkDormantMember(String loginId, String email) {
        String emailHash = sha256Utils.encrypt(email);
        Member member = memberRepository.findByLoginIdAndEmailHash(loginId, emailHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getStatus() != Status.DORMANT) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_DORMANT);
        }
    }

    @Override
    @Transactional
    public void activateDormantMember(String loginId, String email) {
        String emailHash = sha256Utils.encrypt(email);

        Member member = memberRepository.findByLoginIdAndEmailHash(loginId, emailHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));


        if (member.getStatus() == Status.WITHDRAWAL) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (member.getStatus() == Status.ACTIVE) {
            return;
        }

        if (member.getStatus() != Status.DORMANT) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_DORMANT);
        }

        member.setStatus(Status.ACTIVE);
        member.setLastLoginAt(LocalDateTime.now());
    }
}