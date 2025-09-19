package com.memorylab.service.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String body) {
        try {
            JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
            // 디버그를 코드에서 강제로 ON (환경설정과 무관하게 확실히)
            impl.getSession().setDebug(true);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            // From은 Gmail 계정과 반드시 동일하게
            helper.setFrom("dudqja8617@gmail.com", "MemoryLab");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);

            log.info("[MAIL][sending] host={}, port={}, user={}",
                    impl.getHost(), impl.getPort(), impl.getUsername());

            mailSender.send(msg);

            log.info("[MAIL][sent] to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[MAIL][failed] to={}, subject={}, ex={}", to, subject, e.toString(), e);
            throw new RuntimeException("Mail send failed", e);
        }
    }
}
