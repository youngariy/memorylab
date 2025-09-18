package com.memorylab.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime lastSentAt; // 마지막 발송 시간 기록

    private boolean verified;

    public VerificationCode(String email, String code) {
        this.email = email;
        this.code = code;
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
        this.lastSentAt = LocalDateTime.now(); // 생성 시 발송 시간 기록
        this.verified = false;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void updateCode(String newCode) {
        this.code = newCode;
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
        this.lastSentAt = LocalDateTime.now(); // 코드 업데이트 시 발송 시간 갱신
        this.verified = false;
    }
}
