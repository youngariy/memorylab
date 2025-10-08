package com.memorylab.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai-callback")
public class AiCallbackController {

    private final ObjectMapper snakeMapper;
    private final AiResultHandlerService resultHandlerService;
    private final String secret; // 환경변수에서 주입

    public AiCallbackController(AiResultHandlerService resultHandlerService) {
        // ObjectMapper를 snake_case로 별도 준비 (result_url, completed_at 지원)
        this.snakeMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.resultHandlerService = resultHandlerService;
        this.secret = Optional.ofNullable(System.getenv("AI_CALLBACK_SECRET"))
                .orElse(System.getenv("APP_AI_SERVER_SECRET_KEY"));
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notifyCompleted(
            @RequestHeader(name = "X-Signature", required = false) String signature,
            @RequestBody String rawBody
    ) {
        // ===== [SIG_DEBUG] BEGIN =====
        log.info("[SIG_DEBUG] raw.len={} body={}", rawBody != null ? rawBody.length() : -1, rawBody);
        log.info("[SIG_DEBUG] header.sig={}", signature);
        log.info("[SIG_DEBUG] secret.len={}", secret != null ? secret.length() : 0);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody != null ? rawBody.getBytes(StandardCharsets.UTF_8) : new byte[0]);
            String calc = base64UrlNoPad(digest); // Use existing helper
            log.info("[SIG_DEBUG] calc.base64url.nopad={}", calc);
        } catch (Exception e) {
            log.warn("[SIG_DEBUG] calc.error {}", e.toString());
        }
        // ===== [SIG_DEBUG] END =====

        // 1) 시그니처 필수
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err("missing_signature"));
        }
        // 2) 시그니처 검증
        if (!verifyHmac(signature, rawBody, secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err("invalid_signature"));
        }
        // 3) DTO 파싱 (snake_case 지원)
        AiNotificationDto dto;
        try {
            dto = snakeMapper.readValue(rawBody, AiNotificationDto.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err("invalid_json:" + e.getMessage()));
        }
        // 4) 필수값 검증
        if (isBlank(dto.status) || isBlank(dto.taskId) || isBlank(dto.filename)) {
            return ResponseEntity.badRequest().body(err("missing_required_fields"));
        }

        // 5) 상태 분기
        switch (dto.status) {
            case "completed" -> {
                // result_url 상대/절대 모두 허용 → 내부에서 정규화
                String normalized = normalizeResultUrl(dto.resultUrl());
                resultHandlerService.queuePlyDownload(dto.taskId(), normalized);
                return ResponseEntity.ok(new Ack("queued", "PLY 파일 다운로드 큐에 등록"));
            }
            case "failed" -> {
                resultHandlerService.processFailedTask(dto.taskId(), dto.errorCode(), dto.errorDetail());
                return ResponseEntity.ok(new Ack("acknowledged", "실패 상태 반영"));
            }
            default -> {
                return ResponseEntity.badRequest().body(err("unsupported_status:" + dto.status));
            }
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static record Ack(String status, String message) {}

    private static record Err(String error) {}
    private static Err err(String e) { return new Err(e); }

    // 절대/상대 URL 모두 처리
    private String normalizeResultUrl(String input) {
        if (input == null || input.isBlank()) return input;
        if (input.startsWith("http://") || input.startsWith("https://")) return input;
        // 프로토콜 명세: 앞에 https://mlab-api.snowytiger.me/ 가 붙는다고 했음
        return "https://mlab-api.snowytiger.me/" + (input.startsWith("/") ? input.substring(1) : input);
    }

    private boolean verifyHmac(String headerSig, String rawBody, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String calc = base64UrlNoPad(out);
            // 시간 상수 비교
            return MessageDigest.isEqual(calc.getBytes(StandardCharsets.UTF_8), headerSig.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static String base64UrlNoPad(byte[] bytes) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ✅ snake_case DTO
    public static class AiNotificationDto {
        @NotBlank public String status;
        @NotBlank public String taskId;       // task_id 매핑됨 (snakeMapper가 자동 처리)
        @NotBlank public String filename;
        public String resultUrl;              // result_url 매핑됨
        public String completedAt;            // completed_at 매핑됨
        public String errorCode;              // error_code 매핑됨
        public String errorDetail;            // error_detail 매핑됨

        public String status()      { return status; }
        public String taskId()      { return taskId; }
        public String filename()    { return filename; }
        public String resultUrl()   { return resultUrl; }
        public String completedAt() { return completedAt; }
        public String errorCode()   { return errorCode; }
        public String errorDetail() { return errorDetail; }
    }
}
