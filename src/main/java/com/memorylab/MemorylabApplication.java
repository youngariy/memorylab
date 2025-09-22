package com.memorylab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // 비동기 처리를 활성화합니다.
@EnableScheduling // 스케줄링을 활성화합니다.
@SpringBootApplication
public class MemorylabApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemorylabApplication.class, args);
	}

}
