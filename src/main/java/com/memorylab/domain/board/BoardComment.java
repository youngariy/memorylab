// src/main/java/com/memorylab/domain/board/BoardComment.java
package com.memorylab.domain.board;

import com.memorylab.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class BoardComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private Board board;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private User author;

    @Column(nullable=false, length=1000)
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
