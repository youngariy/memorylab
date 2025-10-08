package com.memorylab.controller;

import com.memorylab.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ThumbnailProxyController {

    private final FileService fileService;

    @GetMapping("/thumbnails/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {
            // 썸네일은 정해진 단일 디렉토리에 저장됨
            Path thumbnailDir = Paths.get(fileService.getUploadRootDir().toString(), "thumbnails");
            Path thumbnailPath = thumbnailDir.resolve(filename).normalize();

            Resource resource = new FileSystemResource(thumbnailPath);

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Thumbnail not found or not readable: {}", thumbnailPath);
                // 기본 이미지나 404를 반환할 수 있음
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);

        } catch (Exception e) {
            log.error("Error serving thumbnail file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
