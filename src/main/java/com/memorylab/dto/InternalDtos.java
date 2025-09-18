package com.memorylab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InternalDtos {

    @Getter
    @NoArgsConstructor
    public static class ConversionCallbackRequest {
        @NotNull
        private Long boardId;

        @NotBlank
        private String status; // "COMPLETED" 또는 "FAILED"

        private String thumbnailUrl; // 변환 성공 시 썸네일 URL
    }
}
