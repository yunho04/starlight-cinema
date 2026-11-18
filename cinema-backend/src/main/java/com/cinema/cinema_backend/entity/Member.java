package com.cinema.cinema_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    // DB 컬럼명은 id이지만 의미는 로그인 아이디(API의 login_id)
    @Column(name = "id", nullable = false, length = 80, unique = true)
    private String loginId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    // VARCHAR 컬럼에 'yyyy-MM-dd' 형식으로 저장
    @Column(name = "birth_day", nullable = false, length = 254)
    private String birthDay;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Member(String loginId, String name, LocalDate birthDate,
                  String passwordHash, String phoneNumber, LocalDateTime createdAt) {
        this.loginId = loginId;
        this.name = name;
        this.birthDay = birthDate.toString();
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    public LocalDate getBirthDate() {
        return LocalDate.parse(birthDay.trim());
    }
}
