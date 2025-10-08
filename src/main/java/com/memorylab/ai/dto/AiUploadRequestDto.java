package com.memorylab.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiUploadRequestDto {
    private String filename;
    private String file_url;
}
