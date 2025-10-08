package com.memorylab.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@Primary
public class LocalFileService implements FileService {

    private final Path rootLocation;
    private final Path videoRoot;
    private final Path thumbnailRoot; // 썸네일 루트 경로 추가

    public LocalFileService(@Value("${app.upload.root-dir:/home/ec2-user/app/uploads}") String uploadRootDir) {
        this.rootLocation = Paths.get(uploadRootDir).toAbsolutePath().normalize();
        this.videoRoot = this.rootLocation.resolve("videos").normalize();
        this.thumbnailRoot = this.rootLocation.resolve("thumbnails").normalize(); // 썸네일 경로 초기화
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(videoRoot);
            Files.createDirectories(thumbnailRoot); // 썸네일 디렉토리 생성 로직 추가
        } catch (IOException e) {
            log.error("Could not initialize storage location", e);
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public Path getVideoPath(String uniqueFilename) {
        return videoRoot.resolve(uniqueFilename).normalize();
    }

    @Override
    public void saveFile(MultipartFile file, Path destination) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void deleteDirectory(Path directoryPath) {
        try {
            if (Files.notExists(directoryPath)) {
                return;
            }
            if (Files.isDirectory(directoryPath)) {
                Files.walk(directoryPath)
                     .sorted((p1, p2) -> -p1.compareTo(p2))
                     .forEach(p -> {
                         try {
                             Files.delete(p);
                         } catch (IOException e) {
                             log.error("Failed to delete path: {}", p, e);
                         }
                     });
            } else {
                Files.delete(directoryPath);
            }
            log.info("Successfully deleted path: {}", directoryPath);
        } catch (IOException e) {
            log.error("Error while trying to delete path: {}", directoryPath, e);
        }
    }

    @Override
    public Path getUploadRootDir() {
        return this.rootLocation;
    }
}
