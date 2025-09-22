package com.memorylab.web.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
public class SignatureValidator {

    private final String secretKey;
    private final long timestampValiditySeconds;

    public SignatureValidator(
            @Value("${app.ai-server.secret-key}") String secretKey,
            @Value("${app.ai-server.timestamp-validity-sec:300}") long timestampValiditySeconds
    ) {
        this.secretKey = secretKey;
        this.timestampValiditySeconds = timestampValiditySeconds;
    }

    /**
     * HMAC-SHA256 서명을 검증합니다.
     *
     * @param requestBody    요청 본문 (raw string)
     * @param signatureHeader X-Signature 헤더 값
     * @param timestampHeader X-Timestamp 헤더 값 (Unix epoch seconds)
     * @return 서명이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid(String requestBody, String signatureHeader, String timestampHeader) {
        if (requestBody == null || signatureHeader == null || timestampHeader == null) {
            log.warn("[AUTH] 서명 검증 실패: 필수 헤더 또는 본문이 누락되었습니다.");
            return false;
        }

        // 1. 타임스탬프 유효성 검증 (재전송 공격 방지)
        try {
            long requestTimestamp = Long.parseLong(timestampHeader);
            long currentTimestamp = Instant.now().getEpochSecond();
            if (Math.abs(currentTimestamp - requestTimestamp) > timestampValiditySeconds) {
                log.warn("[AUTH] 서명 검증 실패: 타임스탬프가 유효 범위를 벗어났습니다. request_ts={}, current_ts={}", requestTimestamp, currentTimestamp);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("[AUTH] 서명 검증 실패: 유효하지 않은 타임스탬프 형식입니다. timestamp={}", timestampHeader);
            return false;
        }

        // 2. 서명 생성 및 비교
        try {
            String expectedSignature = generateSignature(requestBody, timestampHeader);
            // 고정 시간 비교(Timing-safe comparison)를 통해 타이밍 공격 방지
            return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[AUTH] 서명 생성 중 심각한 오류 발생", e);
            return false; // 암호화 알고리즘 문제 발생 시 요청 거부
        }
    }

    /**
     * 주어진 데이터로 HMAC-SHA256 서명을 생성합니다.
     */
    private String generateSignature(String data, String timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        String payload = data + "." + timestamp;
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
