package com.memorylab.domain.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users", indexes = {
        @Index(columnList = "email", unique = true),
        @Index(columnList = "nickname", unique = true)
})
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=120)
    private String email;

    @Column(nullable=false)
    private String passwordHash;

    @Column(nullable=false, unique=true, length=40)
    private String nickname;

    @Column(nullable=false, length=60)
    private String name;

    private boolean emailVerified;

    private LocalDateTime createdAt;
    @PrePersist void onCreate(){ createdAt = LocalDateTime.now(); }
}
