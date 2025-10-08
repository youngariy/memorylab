package com.memorylab.service;

import com.memorylab.dto.GpuUploadRequest;
import com.memorylab.dto.GpuUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class GpuApiService {

    private final WebClient webClient;

    public GpuApiService(WebClient.Builder webClientBuilder, @Value("${gpu-server.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * GPU 서버에 동영상 처리를 요청합니다.
     * @param filename 고유 파일명
     * @param fileUrl 동영상 파일 주소 (도메인 제외)
     * @return GpuUploadResponse Mono
     */
    public Mono<GpuUploadResponse> requestProcessing(String filename, String fileUrl) {
        GpuUploadRequest requestBody = new GpuUploadRequest(filename, fileUrl);
        log.info("Requesting GPU processing for filename: {}", filename);
        return webClient.post()
            .uri("/upload")
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(
                status -> status.value() != HttpStatus.ACCEPTED.value(),
                clientResponse -> clientResponse.bodyToMono(String.class).flatMap(body -> {
                    log.error("GPU server upload failed with status {}: {}", clientResponse.statusCode(), body);
                    return Mono.error(new RuntimeException("GPU server upload failed: " + body));
                })
            )
            .bodyToMono(GpuUploadResponse.class)
            .doOnSuccess(response -> log.info("Successfully queued video processing. Task ID: {}", response.getTaskId()));
    }

    /**
     * GPU 서버에 저장된 결과물을 삭제하도록 요청합니다.
     * @param taskId 작업 ID
     * @return 성공 시 true, 실패 시 false를 포함하는 Mono
     */
    public Mono<Boolean> deleteResult(String taskId) {
        log.info("Requesting deletion of task {} from GPU server.", taskId);
        return webClient.delete()
            .uri(uriBuilder -> uriBuilder.path("/delete").build()) // DELETE 요청은 보통 Request Body를 사용하지 않음
            .attribute("task_id", taskId) // 명세가 불분명하므로, URL 파라미터나 헤더로 전달하는 것이 더 일반적임. 여기서는 요청 속성으로만.
            // 실제 API 명세에 따라 아래와 같이 수정해야 할 수 있음:
            // .uri(uriBuilder -> uriBuilder.path("/delete").queryParam("task_id", taskId).build())
            // 또는 헤더로 전달:
            // .header("X-Task-ID", taskId)
            .retrieve()
            .toBodilessEntity()
            .map(response -> response.getStatusCode().is2xxSuccessful())
            .doOnSuccess(success -> {
                if (Boolean.TRUE.equals(success)) {
                    log.info("Deletion request for task {} was successful.", taskId);
                } else {
                    log.warn("Deletion request for task {} failed with non-2xx status.", taskId);
                }
            })
            .onErrorResume(e -> {
                log.error("Error requesting deletion for task {}: {}", taskId, e.getMessage());
                return Mono.just(false);
            });
    }
}
