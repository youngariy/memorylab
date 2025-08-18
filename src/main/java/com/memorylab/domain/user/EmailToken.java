package com.memorylab.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailToken {

    public enum TokenType { VERIFY }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=100)
    private String token;

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private TokenType type;

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    private boolean used;

    public static EmailToken issue(User user, TokenType type, long minutes){
        return EmailToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(minutes))
                .used(false)
                .build();
    }
}
