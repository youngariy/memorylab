package com.memorylab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileService {

    private final Path fileStorageLocation;

    public LocalFileService(@Value("${app.upload.dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("파일명에 부적합한 문자가 포함되어 있습니다: " + originalFileName);
            }
            String fileExtension = "";
            try {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } catch (Exception e) {
                // 확장자가 없는 경우
            }
            String storedFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 성공: {}", storedFileName);
            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("파일 " + originalFileName + "을(를) 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }

    /**
     * 파일을 디스크에서 삭제합니다.
     * @param filename 서버에 저장된 고유한 파일명
     */
    public void deleteFile(String filename) {
        if (!StringUtils.hasText(filename)) {
            return;
        }
        try {
            Path targetLocation = this.fileStorageLocation.resolve(filename);
            Files.deleteIfExists(targetLocation);
            log.info("파일 삭제 성공: {}", filename);
        } catch (IOException ex) {
            log.error("파일 {} 을(를) 삭제할 수 없습니다.", filename, ex);
            // 여기서는 예외를 던지지 않고 로그만 남겨서, 파일 삭제에 실패하더라도
            // 게시글 삭제 등 다른 로직은 계속 진행되도록 할 수 있습니다.
        }
    }
}
