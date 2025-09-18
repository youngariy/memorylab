package com.memorylab.service.event;

// ApplicationEvent 상속 제거, POJO 레코드로 변경
public record VerificationEmailEvent(String email, String code) {}
