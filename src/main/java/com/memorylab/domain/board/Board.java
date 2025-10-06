package com.memorylab.domain.board;

import com.memorylab.domain.BaseTimeEntity;
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

    // --- 신규/대체 필드 ---
    @Column(length = 512)
    private String originalVideoPath;

    @Column(length = 512)
    private String convertedVideoPath;

    @Column(length = 512)
    private String thumbnailPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThumbnailStatus thumbnailStatus = ThumbnailStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscodeStatus transcodeStatus = TranscodeStatus.NONE;

    @Column(nullable = false)
    private int retryCount = 0;

    @Version
    private Long version;

    @Builder
    public Board(Member user, String title, String content, Category category, Visibility visibility) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.category = category;
        this.visibility = visibility;
        this.viewCount = 0;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}
