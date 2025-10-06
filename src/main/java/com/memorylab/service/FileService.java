package com.memorylab.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 파일 저장, 경로 관리, 삭제 등 파일 시스템과 관련된 모든 작업을 추상화하는 인터페이스입니다.
 */
public interface FileService {

    // --- 경로 관리 --- //

    Path getVideoPath(Long boardId);

    Path getThumbnailPath(Long boardId);

    Path getTempThumbnailPath(Long boardId);

    String getRelativeThumbnailUrl(Long boardId);

    Path getVideoDirectory(Long boardId);

    Path getThumbnailDirectory(Long boardId);

    // --- 파일 처리 --- //

    void saveFile(MultipartFile file, Path destination) throws IOException;

    void moveFile(Path source, Path destination) throws IOException;

    void deleteDirectory(Path directoryPath);
}
