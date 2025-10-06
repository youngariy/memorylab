package com.memorylab.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class HashUtils {

    private HashUtils() {}

    /**
     * 입력된 문자열을 SHA-256으로 해시하고 Base64로 인코딩합니다.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 표준 알고리즘이므로 이 예외는 거의 발생하지 않습니다.
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 시간차 공격에 안전한 문자열 비교를 수행합니다.
     */
    public static boolean slowEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
