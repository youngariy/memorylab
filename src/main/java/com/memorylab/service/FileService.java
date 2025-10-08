package com.memorylab.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 파일 저장, 경로 관리, 삭제 등 파일 시스템과 관련된 모든 작업을 추상화하는 인터페이스입니다.
 */
public interface FileService {

    /**
     * 동영상이 저장될 절대 경로를 반환합니다.
     * @param uniqueFilename 고유 파일명 (e.g., "{boardId}_{originalFilename}")
     * @return 저장될 파일의 전체 경로
     */
    Path getVideoPath(String uniqueFilename);

    /**
     * 파일을 저장합니다.
     * @param file 저장할 MultipartFile
     * @param destination 저장될 파일의 전체 경로
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    void saveFile(MultipartFile file, Path destination) throws IOException;

    /**
     * 지정된 디렉토리와 그 하위 내용을 모두 삭제합니다.
     * @param directoryPath 삭제할 디렉토리 경로
     */
    void deleteDirectory(Path directoryPath);

    /**
     * 프로젝트의 업로드 루트 디렉토리 경로를 반환합니다.
     * @return 루트 디렉토리 Path 객체
     */
    Path getUploadRootDir();
}
