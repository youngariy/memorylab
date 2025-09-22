package com.memorylab.domain.board;

import com.memorylab.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(indexes = {
        @Index(name="idx_board_category", columnList="category"),
        @Index(name="idx_board_visibility", columnList="visibility"),
        @Index(name="idx_board_createdAt", columnList="createdAt DESC"),
        @Index(name="idx_board_conversion_status", columnList="conversionStatus")
})
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version; // 낙관적 락을 위한 버전 필드

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

    // === 동영상 및 변환 관련 필드 ===
    @Column(length = 512)
    private String originalVideoPath;

    @Column(length = 512)
    private String convertedVideoPath;

    @Column(length = 512)
    private String thumbnailPath;

    @Column(length = 36)
    private String jobId; // 변환 작업을 식별하기 위한 고유 ID

    @Column(length = 255)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConversionStatus conversionStatus;

    @Lob
    private String errorMessage;

    private Integer progress;

    private int retryCount;
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
        this.errorMessage = null;
        if (status == ConversionStatus.PROCESSING) {
            this.progress = 0;
        } else {
            this.progress = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void updateOnCompletion(String thumbnailPath, String convertedVideoPath) {
        this.conversionStatus = ConversionStatus.COMPLETED;
        this.thumbnailPath = thumbnailPath;
        this.convertedVideoPath = convertedVideoPath;
        this.progress = 100;
        this.errorMessage = null;
        this.retryCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateOnError(String message) {
        this.conversionStatus = ConversionStatus.ERROR;
        this.errorMessage = message;
        this.progress = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeVideo(String newOriginalVideoPath) {
        this.originalVideoPath = newOriginalVideoPath;
        this.convertedVideoPath = null;
        this.thumbnailPath = null;
        this.conversionStatus = ConversionStatus.PENDING;
        this.progress = null;
        this.errorMessage = null;
        this.retryCount = 0;
        this.jobId = UUID.randomUUID().toString(); // 새 작업이므로 새 Job ID 생성
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProgress(Integer progress) {
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }

    public void resetForRetry() {
        this.conversionStatus = ConversionStatus.PENDING;
        this.errorMessage = null;
        this.progress = null;
        this.convertedVideoPath = null;
        this.thumbnailPath = null;
        this.retryCount = 0;
        this.jobId = UUID.randomUUID().toString(); // 재시도도 새 Job ID 생성
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
