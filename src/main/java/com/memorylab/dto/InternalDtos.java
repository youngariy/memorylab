package com.memorylab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InternalDtos {

    /** 변환 완료/실패 시 AI 서버가 호출하는 콜백 DTO */
    @Getter
    @NoArgsConstructor
    public static class ConversionCallbackRequest {
        @NotNull
        private Long boardId;
        @NotBlank
        private String jobId;

        @NotBlank
        private String status; // "COMPLETED" 또는 "ERROR"

        private String thumbnailUrl;
        private String convertedVideoUrl;
        private String errorMessage;
    }

    /** 변환 진행 중 AI 서버가 주기적으로 호출하는 콜백 DTO */
    @Getter
    @NoArgsConstructor
    public static class ConversionProgressRequest {
        @NotNull
        private Long boardId;
        @NotBlank
        private String jobId;

        @NotNull
        @Min(0)
        @Max(100)
        private Integer progress;
    }

    /** 폴링 시 AI 서버의 상태 응답을 매핑하기 위한 DTO */
    @Getter
    @NoArgsConstructor
    public static class AiServerStatusResponse {
        private String jobId;
        private String status; // "processing", "completed", "error"
        private Integer percentage;
        private String resultUrl;
        private String thumbnailUrl;
        private String errorMessage;
    }
}
