package com.memorylab.service;

import com.memorylab.ai.AiServerClient;
import com.memorylab.domain.BoardStatus;
import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final BoardRepository boardRepository;
    private final WebClient.Builder webClientBuilder;
    private final AiServerClient aiServerClient; // GpuApiService 대신 AiServerClient 주입

    @Value("${file.storage.ply-base-path}")
    private String plyBasePath;

    @Value("${file.storage.tmp-path}")
    private String tmpPath;

    @Value("${file.download.allowed-domains}")
    private List<String> allowedDomains;

    @Value("${file.download.min-free-space-mb}")
    private long minFreeSpaceMb;

    private final Semaphore downloadSemaphore = new Semaphore(5);

    public void downloadPlyFile(Board board, String resultUrl) {
        if (!downloadSemaphore.tryAcquire()) {
            log.info("Maximum concurrent downloads reached. Deferring download for boardId: {}", board.getId());
            return;
        }

        Path tempFilePath = Paths.get(tmpPath, board.getId() + ".ply.part");

        try {
            if (!isUrlValid(resultUrl)) {
                throw new IllegalArgumentException("Invalid result URL: " + maskUrl(resultUrl));
            }
            Path finalDir = Paths.get(plyBasePath, String.valueOf(board.getId()));
            Files.createDirectories(finalDir);
            if (!hasEnoughDiskSpace(finalDir)) {
                throw new IOException("Not enough disk space");
            }

            updateStatus(board.getId(), BoardStatus.DOWNLOADING, null);

            WebClient client = webClientBuilder.baseUrl(getFullBaseUrl(resultUrl)).build();
            Flux<DataBuffer> dataFlux = client.get()
                .uri(getUriPath(resultUrl))
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(10)).maxBackoff(Duration.ofMinutes(1)));

            DataBufferUtils.write(dataFlux, tempFilePath)
                .then(Mono.fromRunnable(() -> {
                    try {
                        finalizeDownload(board, tempFilePath, finalDir);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }))
                .doOnError(error -> {
                    log.error("Download or finalization failed for boardId: {}. Reason: {}", board.getId(), error.getMessage());
                    updateStatus(board.getId(), BoardStatus.FAILED_DOWNLOAD, null);
                    deleteTempFile(tempFilePath);
                })
                .doFinally(signalType -> downloadSemaphore.release())
                .subscribe();

        } catch (Exception e) {
            log.error("Pre-download check failed for boardId: {}. Reason: {}", board.getId(), e.getMessage());
            updateStatus(board.getId(), BoardStatus.FAILED_DOWNLOAD, null);
            downloadSemaphore.release();
        }
    }

    private void finalizeDownload(Board board, Path tempFilePath, Path finalDir) throws IOException {
        Path finalFilePath = finalDir.resolve(board.getId() + ".ply");
        Files.move(tempFilePath, finalFilePath, StandardCopyOption.ATOMIC_MOVE);
        log.info("File for boardId: {} moved to final destination: {}", board.getId(), finalFilePath);

        String relativePath = "/ply/" + board.getId() + "/" + board.getId() + ".ply";
        updateStatus(board.getId(), BoardStatus.READY, relativePath);

        // 다운로드 성공 후 AI 서버에 삭제 요청
        aiServerClient.deleteAiResource(board.getAiTaskId());
    }

    @Transactional
    public void updateStatus(Long boardId, BoardStatus status, String plyPath) {
        boardRepository.findById(boardId).ifPresent(board -> {
            board.setStatus(status);
            if (plyPath != null) {
                board.setPlyPath(plyPath);
            }
        });
    }

    private boolean isUrlValid(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain == null) return true; // 상대 경로인 경우
            return allowedDomains.stream().anyMatch(domain::equalsIgnoreCase);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String getFullBaseUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        if (!uri.isAbsolute()) return "";
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    private String getUriPath(String url) throws URISyntaxException {
        URI uri = new URI(url);
        if (!uri.isAbsolute()) return url;
        return uri.getPath();
    }

    private boolean hasEnoughDiskSpace(Path path) {
        File file = path.toFile();
        long requiredSpace = minFreeSpaceMb * 1024 * 1024;
        return file.getUsableSpace() > requiredSpace;
    }

    private void deleteTempFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temporary file: {}", path, e);
        }
    }

    public String maskUrl(String url) {
        if (url == null) return null;
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute()) return url;
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            return "<invalid-url>";
        }
    }
}
