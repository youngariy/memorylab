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

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Category category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    // === 동영상 및 변환 관련 필드 추가 ===
    @Column(length = 512) // 로컬 경로이므로 S3 URL보다 짧아도 됨
    private String videoUrl;

    @Column(length = 512)
    private String thumbnailUrl;

    @Column(length = 255)
    private String tags;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ConversionStatus conversionStatus = ConversionStatus.NOT_STARTED;
    // =====================================

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

    public void modify(String title, String content, Category category, Visibility visibility, String tags) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.visibility = visibility;
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateConversionStatus(ConversionStatus status) {
        this.conversionStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateConversionResult(ConversionStatus status, String thumbnailUrl) {
        this.conversionStatus = status;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }

    // === 동영상 교체 및 상태 리셋을 위한 메소드 추가 ===
    public void changeVideo(String newVideoUrl) {
        this.videoUrl = newVideoUrl;
        this.thumbnailUrl = null; // 새 영상이므로 기존 썸네일은 무효
        this.conversionStatus = ConversionStatus.UPLOADED; // 변환 상태 초기화
        this.updatedAt = LocalDateTime.now();
    }
}
