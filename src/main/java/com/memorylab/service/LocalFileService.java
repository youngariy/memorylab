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
    // 새 썸네일 경로
    private static final Path THUMB_ROOT = Paths.get("/home/ec2-user/app/data/thumbnails");

    public LocalFileService(@Value("${app.upload.root-dir:/home/ec2-user/app/uploads}") String uploadRootDir) {
        this.rootLocation = Paths.get(uploadRootDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation.resolve("videos"));
            // 새 썸네일 경로 생성
            Files.createDirectories(THUMB_ROOT);
        } catch (IOException e) {
            log.error("Could not initialize storage location", e);
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public Path getVideoPath(Long boardId) {
        return rootLocation.resolve(Paths.get("videos", String.valueOf(boardId), "video.mp4")).normalize();
    }

    @Override
    public Path getThumbnailPath(Long boardId) {
        // 새 경로 구조에 맞게 수정
        return THUMB_ROOT.resolve(boardId + ".jpg");
    }

    @Override
    public Path getTempThumbnailPath(Long boardId) {
        // 새 경로 구조에 맞게 수정
        return THUMB_ROOT.resolve(boardId + ".tmp.jpg");
    }

    @Override
    public String getRelativeThumbnailUrl(Long boardId) {
        // 새 경로 구조에 맞게 수정
        return "/thumbnails/" + boardId + ".jpg";
    }

    @Override
    public Path getVideoDirectory(Long boardId) {
        return rootLocation.resolve(Paths.get("videos", String.valueOf(boardId))).normalize();
    }

    @Override
    public Path getThumbnailDirectory(Long boardId) {
        // 새 썸네일 루트 디렉토리 반환
        return THUMB_ROOT;
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
    public void moveFile(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        log.info("Atomically moved file from {} to {}", source, destination);
    }

    @Override
    public void deleteDirectory(Path directoryPath) {
        try {
            if (Files.exists(directoryPath)) {
                Files.walk(directoryPath)
                     .sorted((p1, p2) -> -p1.compareTo(p2))
                     .forEach(p -> {
                         try {
                             Files.delete(p);
                         } catch (IOException e) {
                             log.error("Failed to delete path: {}", p, e);
                         }
                     });
                log.info("Successfully deleted directory: {}", directoryPath);
            }
        } catch (IOException e) {
            log.error("Error while trying to delete directory: {}", directoryPath, e);
        }
    }
}
