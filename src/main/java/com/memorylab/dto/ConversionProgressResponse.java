package com.memorylab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConversionProgressResponse {

    private String status; // e.g., "standby", "processing", "completed", "error"

    private int progress; // 0-100

    @JsonProperty("result_url")
    private String resultUrl;

    @JsonProperty("sha256")
    private String sha256; // 파일 무결성 검증을 위한 SHA-256 해시
}
