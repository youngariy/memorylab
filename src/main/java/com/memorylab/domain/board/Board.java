// src/main/java/com/memorylab/domain/board/Board.java
package com.memorylab.domain.board;

import com.memorylab.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(indexes = {
        @Index(name="idx_board_category", columnList="category"),
        @Index(name="idx_board_visibility", columnList="visibility"),
        @Index(name="idx_board_createdAt", columnList="createdAt DESC")
})
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private User author;

    @Column(nullable=false, length=120)
    private String title;

    @Lob
    @Column(nullable=false)
    private String content;

    // ==== 변경된 부분: 카테고리 Enum ====
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Category category;

    @Builder.Default                 // ★ Lombok Builder 기본값 유지
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    private long viewCount;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==== 유틸 메서드 ====
    public void increaseView() { this.viewCount++; }

    public boolean isAuthor(Long userId) {
        return author != null && author.getId().equals(userId);
    }

    public void modify(String title, String content, Category category, Visibility visibility) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.visibility = visibility;
        this.updatedAt = LocalDateTime.now();
    }
}
