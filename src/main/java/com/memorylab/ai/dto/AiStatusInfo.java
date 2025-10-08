package com.memorylab.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class AiStatusInfo {
    private String status;
    private TaskInfo task;
    private String message;

    @Getter
    @ToString
    @NoArgsConstructor
    public static class TaskInfo {
        @JsonProperty("result_url")
        private String resultUrl;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_detail")
        private String errorDetail;
    }
}
