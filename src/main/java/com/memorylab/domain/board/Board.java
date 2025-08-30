// src/main/java/com/memorylab/domain/board/Board.java
package com.memorylab.domain.board;

import com.memorylab.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity @Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(indexes = {
        @Index(name="idx_board_category", columnList="category"),
        @Index(name="idx_board_createdAt", columnList="createdAt DESC")
})
public class Board {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private User author;

    @Column(nullable=false, length=120)
    private String title;

    @Lob @Column(nullable=false)
    private String content;

    @Column(length=50)
    private String category;

    private long viewCount;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void increaseView(){ this.viewCount++; }
    public boolean isAuthor(Long userId){ return author!=null && author.getId().equals(userId); }
    public void modify(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }
}
