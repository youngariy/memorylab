package com.memorylab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GpuTaskDetail {

    @JsonProperty("task_id")
    private String taskId;

    private String filename;

    @JsonProperty("result_url")
    private String resultUrl;

    @JsonProperty("queued_at")
    private ZonedDateTime queuedAt;

    @JsonProperty("started_at")
    private ZonedDateTime startedAt;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;
}
