package com.memorylab.web.internal;

import com.memorylab.dto.InternalDtos.ConversionCallbackRequest;
import com.memorylab.service.board.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final BoardService boardService;

    @Value("${app.ai-server.secret-key}")
    private String aiServerSecretKey;

    /**
     * AI 서버가 동영상 변환 결과를 콜백하는 엔드포인트
     */
    @PostMapping("/conversion-callback")
    public ResponseEntity<Void> handleConversionCallback(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid ConversionCallbackRequest request
    ) {
        // 1. Secret Key 검증
        if (!StringUtils.hasText(authorization) || !authorization.equals("Bearer " + aiServerSecretKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. 서비스 로직 호출
        boardService.processConversionResult(request);

        return ResponseEntity.ok().build();
    }
}
