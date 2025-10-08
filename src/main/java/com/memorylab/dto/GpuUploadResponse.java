package com.memorylab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GpuUploadResponse {

    private String status;

    @JsonProperty("task_id")
    private String taskId;

    private String message;
}
