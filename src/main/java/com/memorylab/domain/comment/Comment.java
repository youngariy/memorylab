package com.memorylab.domain.comment;

import com.memorylab.domain.BaseTimeEntity;
import com.memorylab.domain.board.Board;
import com.memorylab.domain.user.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member user;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Comment(Board board, Member user, String content) {
        this.board = board;
        this.user = user;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
