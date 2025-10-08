package com.memorylab.domain.board;

import com.memorylab.domain.BaseTimeEntity;
import com.memorylab.domain.BoardStatus;
import com.memorylab.domain.user.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Board extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member user;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @Column(columnDefinition = "integer default 0")
    private int viewCount;

    @Formula("(select count(1) from board_like bl where bl.board_id = id)")
    private int likeCount;

    @Formula("(select count(1) from comment c where c.board_id = id)")
    private int commentCount;

    // --- 영상/썸네일/PLY 관련 필드 ---
    @Column(length = 512)
    private String originalVideoPath;

    @Column(length = 512)
    private String convertedVideoPath;

    @Column(length = 512)
    private String thumbnailPath;

    @Column(length = 512)
    private String plyPath; // 로컬에 저장된 PLY 파일 경로

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscodeStatus transcodeStatus = TranscodeStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThumbnailStatus thumbnailStatus = ThumbnailStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BoardStatus status; // 내부 처리 상태

    @Column(nullable = false)
    private int retryCount = 0;

    // --- AI 서버 연동 필드 ---
    @Column(length = 64, unique = true)
    private String aiTaskId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExternalStatus externalStatus;

    @Column(columnDefinition = "TEXT")
    private String externalResultUrl;

    @Column(length = 64)
    private String externalErrorCode;

    @Column(columnDefinition = "TEXT")
    private String externalErrorDetail;

    @Column(length = 255)
    private String gpuErrorMessage; // 레거시 오류 메시지 필드

    @Version
    private Long version;

    @Builder
    public Board(Member user, String title, String content, Category category, Visibility visibility, String originalVideoPath) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.category = category;
        this.visibility = visibility;
        this.originalVideoPath = originalVideoPath;
        this.viewCount = 0;
        this.status = BoardStatus.DISPATCHED; // 빌더는 기존 상태로 초기화
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}
