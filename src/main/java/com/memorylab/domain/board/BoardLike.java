package com.memorylab.domain.board;

import com.memorylab.domain.user.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "board_like")
public class BoardLike {

    @EmbeddedId
    private BoardLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("boardId") // BoardLikeId의 boardId 필드를 board 외래키와 매핑
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // BoardLikeId의 userId 필드를 user 외래키와 매핑
    @JoinColumn(name = "user_id")
    private Member user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BoardLike(Board board, Member user) {
        this.id = new BoardLikeId(board.getId(), user.getId());
        this.board = board;
        this.user = user;
    }
}
