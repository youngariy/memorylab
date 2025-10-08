package com.memorylab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GpuUploadRequest {

    private String filename;

    @JsonProperty("file_url")
    private String fileUrl;
}
