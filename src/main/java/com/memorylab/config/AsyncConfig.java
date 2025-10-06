package com.memorylab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "thumbnailTaskExecutor")
    public Executor thumbnailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 기본 스레드 수: CPU 코어 수
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        // 최대 스레드 수: CPU 코어 수 * 2
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        // 큐 용량
        executor.setQueueCapacity(50);
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("thumb-exec-");
        executor.initialize();
        return executor;
    }
}
