package com.memorylab.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MeResponse(
        Long id,
        String email,
        String name,
        String nickname,
        List<String> roles,
        LocalDateTime createdAt
) {}
