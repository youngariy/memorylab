package com.memorylab.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiTaskResponseDto {
    private String status;
    private String task_id;
    private String message;
}
