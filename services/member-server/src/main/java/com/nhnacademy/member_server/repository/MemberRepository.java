package com.nhnacademy.member_server.repository;

import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Status;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmailHash(String emailHash);

    boolean existsByPhoneHash(String phoneHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT m.id FROM Member m WHERE MONTH(m.birthDate) = :month")
    List<Long> findAllIdsByBirthMonth(@Param("month") int month);

    @Query("SELECT m.id, m.loginId, m.name FROM Member m WHERE m.id IN :memberIds")
    List<Object[]> findSimpleMembers(@Param("memberIds") List<Long> memberIds);

    Optional<Member> findByProviderId(String providerId);

    Optional<Member> findByEmailHash(String emailHash);

    Optional<Member> findByLoginIdAndEmailHash(String loginId, String emailHash);

    @Modifying(clearAutomatically = true) // 영속성 컨텍스트 초기화 필수
    @Query("UPDATE Member m SET m.status = :targetStatus WHERE m.lastLoginAt < :cutOffDate AND m.status = :currentStatus")
    int bulkUpdateDormantMembers(@Param("cutOffDate") LocalDateTime cutOffDate,
                                 @Param("currentStatus") Status currentStatus,
                                 @Param("targetStatus") Status targetStatus);

    Page<Member> findAllByStatus(Status status, Pageable pageable);
}