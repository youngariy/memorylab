package com.memorylab.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class AiNotificationDto {

    private String status;

    @JsonProperty("task_id")
    private String taskId;

    private String filename;

    @JsonProperty("result_url")
    private String resultUrl;

    @JsonProperty("completed_at")
    private String completedAt;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_detail")
    private String errorDetail;
}
