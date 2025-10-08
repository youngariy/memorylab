package com.memorylab.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CleanupScheduler {

    @Value("${file.storage.tmp-path}")
    private String tmpPath;

    /**
     * 매일 새벽 3시에 실행되어 24시간 이상된 임시 파일을 정리합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldTempFiles() {
        log.info("Starting cleanup task for old temporary files...");
        Path tempDir = Paths.get(tmpPath);
        if (!Files.isDirectory(tempDir)) {
            log.warn("Temporary directory not found: {}", tmpPath);
            return;
        }

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        AtomicInteger deleteCount = new AtomicInteger(0);

        try {
            Files.walk(tempDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".part"))
                .forEach(path -> {
                    try {
                        FileTime lastModified = Files.getLastModifiedTime(path);
                        if (lastModified.toInstant().isBefore(cutoff)) {
                            Files.delete(path);
                            log.info("Deleted old temporary file: {}", path);
                            deleteCount.incrementAndGet();
                        }
                    } catch (IOException e) {
                        log.error("Failed to delete temporary file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error walking temporary file directory: {}", tmpPath, e);
        }

        log.info("Cleanup task finished. Deleted {} old temporary files.", deleteCount.get());
    }
}
