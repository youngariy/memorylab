package com.memorylab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 스케줄러 기능 활성화
@EnableJpaAuditing // JPA Auditing 기능 활성화
@SpringBootApplication
public class MemorylabApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemorylabApplication.class, args);
    }

}
