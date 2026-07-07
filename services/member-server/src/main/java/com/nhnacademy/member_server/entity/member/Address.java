package com.nhnacademy.member_server.entity.member;

import com.nhnacademy.member_server.utils.EncryptConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @Setter
    @Column(length = 50, nullable = false)
    private String alias;

    @Setter
    @Column(length = 255, nullable = false)
    @Convert(converter = EncryptConverter.class)
    private String recipient;

    @Setter
    @Column(length = 255, nullable = false)
    @Convert(converter = EncryptConverter.class)
    private String phone;


    @Setter
    @Column(name = "zip_code", length = 10, nullable = false)
    private String zipCode;

    @Setter
    @Column(name = "road_address", length = 255, nullable = false)
    @Convert(converter = EncryptConverter.class)
    private String roadAddress;

    @Setter
    @Column(name = "detail_address", length = 255, nullable = false)
    @Convert(converter = EncryptConverter.class)
    private String detailAddress;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}