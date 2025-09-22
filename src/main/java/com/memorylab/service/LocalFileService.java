package com.memorylab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileService implements FileService {

    private final Path rootDir;

    public LocalFileService(@Value("${app.upload.root-dir}") String rootDir) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (Exception ex) {
            throw new RuntimeException("최상위 업로드 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, FileType fileType) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            return store(file.getInputStream(), fileType, originalFileName);
        } catch (IOException ex) {
            throw new RuntimeException("파일 " + originalFileName + "의 InputStream을 얻을 수 없습니다.", ex);
        }
    }

    @Override
    public String storeFile(byte[] fileData, FileType fileType, String originalFileName) {
        return store(new ByteArrayInputStream(fileData), fileType, originalFileName);
    }

    private String store(InputStream inputStream, FileType fileType, String originalFileName) {
        try {
            if (originalFileName != null && originalFileName.contains("..")) {
                throw new RuntimeException("파일명에 부적합한 문자가 포함되어 있습니다: " + originalFileName);
            }

            Path targetDir = this.rootDir.resolve(fileType.getDirectoryName()).normalize();
            Files.createDirectories(targetDir);

            String fileExtension = StringUtils.getFilenameExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + (fileExtension != null ? "." + fileExtension : "");

            Path targetLocation = targetDir.resolve(storedFileName);
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = Paths.get(fileType.getDirectoryName()).resolve(storedFileName).toString().replace("\\", "/");
            log.info("파일 저장 성공: {}", relativePath);
            return relativePath;

        } catch (IOException ex) {
            throw new RuntimeException("파일 " + originalFileName + "을(를) 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }

    @Override
    public void deleteFile(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        try {
            Path targetLocation = this.rootDir.resolve(relativePath).normalize();
            Files.deleteIfExists(targetLocation);
            log.info("파일 삭제 성공: {}", relativePath);
        } catch (IOException ex) {
            log.error("파일 {} 을(를) 삭제할 수 없습니다.", relativePath, ex);
        }
    }
}
