package com.memorylab.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GpuStatusResponse {

    private String status;
    private GpuTaskDetail task;
    private String message;
}
