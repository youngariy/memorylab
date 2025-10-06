package com.memorylab.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users", indexes = {
        @Index(columnList = "email", unique = true),
        @Index(columnList = "nickname", unique = true)
})
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=120)
    private String email;

    @Column(nullable=false)
    private String password;

    @Column(nullable=false, unique=true, length=40)
    private String nickname;

    @Column(nullable=false, length=60)
    private String name;

    private boolean emailVerified;

    // === roles 필드 매핑 정보 명시 ===
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles", // 대상 테이블 이름
            joinColumns = @JoinColumn(name = "user_id") // 외래 키 컬럼 이름
    )
    @Column(name = "roles") // 값 컬럼 이름
    @Builder.Default
    private List<String> roles = new ArrayList<>();
    // =================================

    private LocalDateTime createdAt;
    @PrePersist void onCreate(){ createdAt = LocalDateTime.now(); }
}
