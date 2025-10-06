package com.memorylab.domain.board;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class BoardLikeId implements Serializable {

    private Long boardId;
    private Long userId;

    public BoardLikeId(Long boardId, Long userId) {
        this.boardId = boardId;
        this.userId = userId;
    }
}
