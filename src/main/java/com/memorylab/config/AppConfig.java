package com.memorylab.config;

import com.memorylab.config.task.MdcTaskDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${app.ai-server.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${app.ai-server.read-timeout:30000}")
    private long readTimeout;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    @Bean(name = "videoConversionTaskExecutor")
    public TaskExecutor videoConversionTaskExecutor(
            @Value("${app.scheduler.concurrency:3}") int concurrency
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setThreadNamePrefix("conversion-thread-");
        // 비동기 작업에 MDC 컨텍스트를 전파하기 위한 Decorator 설정
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}
