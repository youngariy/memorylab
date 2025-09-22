package com.memorylab.web.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorylab.dto.InternalDtos.ConversionCallbackRequest;
import com.memorylab.dto.InternalDtos.ConversionProgressRequest;
import com.memorylab.service.board.BoardService;
import com.memorylab.web.security.SignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final BoardService boardService;
    private final SignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;

    /**
     * AI 서버가 동영상 변환 **완료/실패** 결과를 콜백하는 엔드포인트 (HMAC 서명 검증 적용)
     */
    @PostMapping("/conversion-callback")
    public ResponseEntity<Void> handleConversionCallback(
            @RequestBody String requestBody,
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") String timestamp
    ) {
        // 1. 서명 검증
        if (!signatureValidator.isValid(requestBody, signature, timestamp)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. DTO 변환 및 서비스 로직 호출
        try {
            ConversionCallbackRequest request = objectMapper.readValue(requestBody, ConversionCallbackRequest.class);
            boardService.processConversionResult(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("콜백 요청 본문 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * AI 서버가 동영상 변환 **진행률**을 콜백하는 엔드포인트 (HMAC 서명 검증 적용)
     */
    @PostMapping("/conversion-progress")
    public ResponseEntity<Void> handleConversionProgress(
            @RequestBody String requestBody,
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") String timestamp
    ) {
        // 1. 서명 검증
        if (!signatureValidator.isValid(requestBody, signature, timestamp)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. DTO 변환 및 서비스 로직 호출
        try {
            ConversionProgressRequest request = objectMapper.readValue(requestBody, ConversionProgressRequest.class);
            boardService.updateConversionProgress(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("진행률 콜백 요청 본문 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
