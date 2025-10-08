package com.memorylab.controller;

import com.memorylab.dto.GpuWebhookPayload;
import com.memorylab.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class GpuWebhookController {

    private final WebhookService webhookService;

    @Value("${gpu-server.webhook-secret}")
    private String webhookSecret;

    // GPU 서버에서 요청 시 이 헤더에 시크릿을 담아 보내야 함
    private static final String SIGNATURE_HEADER = "X-Mlab-Signature";

    @PostMapping("/gpu-server")
    public ResponseEntity<Map<String, String>> handleGpuWebhook(
        @RequestBody GpuWebhookPayload payload,
        @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature)
    {
        // 1. 시그니처(시크릿) 검증
        if (!StringUtils.hasText(signature) || !signature.equals(webhookSecret)) {
            log.warn("Invalid or missing signature in webhook request.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Received valid webhook for task ID: {}. Status: {}", payload.getTaskId(), payload.getStatus());

        // 2. 실제 처리는 서비스에 위임
        try {
            webhookService.processWebhook(payload);
            // 3. 프로토콜에 따라 정상 수신 응답 반환
            Map<String, String> responseBody = Map.of(
                "status", "queued",
                "message", "Webhook payload has been successfully queued for processing."
            );
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            log.error("Error processing webhook for task ID: {}", payload.getTaskId(), e);
            Map<String, String> errorBody = Map.of("status", "error", "message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
}
