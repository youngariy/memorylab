package com.memorylab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // application.yml에 설정한 파일 시스템 경로
    @Value("${app.upload.dir}")
    private String uploadDir;

    // application.yml에 설정한 URL 경로
    @Value("${app.upload.resource-handler}")
    private String resourceHandler;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /media/** URL 요청이 오면 file:/var/www/uploads/videos/ 경로에서 파일을 찾아 제공
        registry.addResourceHandler(resourceHandler)
                .addResourceLocations("file:" + uploadDir);
    }
}
