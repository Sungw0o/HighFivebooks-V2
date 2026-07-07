package com.nhnacademy.member_server.entity.member;

import com.nhnacademy.member_server.utils.EncryptConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "member", indexes = {
        @Index(name = "idx_last_login_at", columnList = "last_login_at")
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @NotNull
    @Column(name = "login_id", unique = true, nullable = false, length = 50)
    private String loginId;

    @Setter
    @NotNull
    @Column(nullable = false, length = 255)
    @Convert(converter = EncryptConverter.class)
    private String name;

    @Setter
    @NotNull
    @Column(nullable = false)
    private String password;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    @Builder.Default
    private Gender gender = Gender.UNKNOWN;

    @Setter
    @NotNull
    @Column(nullable = false, length = 255)
    @Convert(converter = EncryptConverter.class)
    private String phone;

    @Setter
    @Column(name = "phone_hash", unique = true, length = 64)
    private String phoneHash;

    @Setter
    @NotNull
    @Column(nullable = false, length = 255)
    @Convert(converter = EncryptConverter.class)
    private String email;

    @Setter
    @Column(name = "email_hash", unique = true, length = 64)
    private String emailHash;

    @Setter
    @NotNull
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Setter
    @NotNull
    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Setter
    @Column(name = "provider_id")
    private String providerId;

    @Setter
    private String provider;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Setter
    @NotNull
    @Column(name = "current_point", nullable = false)
    private long currentPoint;


    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @Setter
    @Column(name = "default_address_id")
    private Long defaultAddressId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    public void addAddress(Address address) {
        this.addresses.add(address);
        address.setMember(this);
    }

    @Setter
    @Column(nullable = false)
    @Builder.Default
    private boolean isProfileComplete = false;
}