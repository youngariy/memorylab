package com.memorylab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GpuWebhookPayload {

    private String status;

    @JsonProperty("task_id")
    private String taskId;

    private String filename;

    // 'completed' 상태일 때 존재
    @JsonProperty("result_url")
    private String resultUrl;

    @JsonProperty("completed_at")
    private ZonedDateTime completedAt;

    // 'failed' 상태일 때 존재
    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_detail")
    private String errorDetail;

    @JsonProperty("failed_at")
    private ZonedDateTime failedAt;
}
