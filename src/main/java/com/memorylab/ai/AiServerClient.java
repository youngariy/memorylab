package com.memorylab.ai;

import com.memorylab.ai.dto.AiDeleteRequestDto;
import com.memorylab.ai.dto.AiTaskResponseDto;
import com.memorylab.ai.dto.AiUploadRequestDto;
import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ExternalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final WebClient webClient;
    private final BoardRepository boardRepository;

    @Value("${gpu-server.base-url}")
    private String aiServerBaseUrl;

    public void requestUpload(Board board, String fileUrl) {
        // filename: 전체 경로에서 파일명만 추출 (예: /path/to/UUID.mp4 -> UUID.mp4)
        String fullPath = board.getOriginalVideoPath();
        String filename = "";
        if (fullPath != null) {
            int lastSlash = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
            filename = lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
        }

        // 프로토콜: filename은 UUID.mp4, file_url은 uploads/videos/UUID.mp4
        AiUploadRequestDto requestDto = new AiUploadRequestDto(filename, fileUrl);

        log.info("AI 서버 업로드 요청 준비: filename={}, file_url={}", filename, fileUrl);

        webClient.post()
                .uri(aiServerBaseUrl + "/upload")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestDto)
                .retrieve()
                .onStatus(
                        HttpStatus.ACCEPTED::equals,
                        response -> response.bodyToMono(AiTaskResponseDto.class)
                                .doOnNext(taskResponse -> {
                                    log.info("AI 서버 업로드 요청 성공 (202 Accepted): taskId={}, status={}", taskResponse.getTask_id(), taskResponse.getStatus());
                                    board.setAiTaskId(taskResponse.getTask_id());
                                    board.setExternalStatus(ExternalStatus.QUEUED);
                                    boardRepository.save(board);
                                })
                                .then(Mono.empty())
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(errorBody -> {
                                    log.error("AI 서버 업로드 요청 실패: status={}, body={}", response.statusCode(), errorBody);
                                    // 필요 시, 여기서 게시글 상태를 FAILED로 업데이트 할 수 있습니다.
                                })
                                .then(Mono.error(new RuntimeException("AI 서버 업로드 실패: " + response.statusCode())))
                )
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("AI 서버 통신 중 오류 발생", error))
                .onErrorResume(e -> Mono.empty()) // 에러가 파이프라인을 중단시키지 않도록 처리
                .subscribe();
    }

    public void deleteAiResource(String taskId) {
        AiDeleteRequestDto requestDto = new AiDeleteRequestDto(taskId, null);

        webClient.method(HttpMethod.DELETE)
                .uri(aiServerBaseUrl + "/task")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestDto)
                .retrieve()
                .onStatus(
                        HttpStatus.OK::equals,
                        response -> {
                            log.info("AI 서버 리소스 삭제 요청 성공: taskId={}", taskId);
                            return Mono.empty();
                        }
                )
                .onStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        response -> response.bodyToMono(String.class)
                                .doOnNext(errorBody -> log.error("AI 서버 리소스 삭제 요청 실패 (500): taskId={}, body={}", taskId, errorBody))
                                .then(Mono.error(new RuntimeException("AI 서버 리소스 삭제 실패: " + response.statusCode())))
                )
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("AI 서버 삭제 통신 중 오류 발생", error))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}
