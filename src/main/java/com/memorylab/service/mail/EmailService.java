package com.memorylab.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {
    public void send(String to, String subject, String body){
        // 개발 단계: 콘솔 로그로 대체
        log.info("[MAIL] to={}, subject={}, body={}", to, subject, body);
    }
}
