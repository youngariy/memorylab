package com.memorylab.config;

import com.memorylab.service.FileType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.converted-base-url:/media/converted}")
    private String convertedBaseUrl;

    @Value("${app.upload.root-dir}")
    private String rootDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 원본(originals) 디렉토리는 외부에 노출하지 않고,
        // 변환된(converted) 디렉토리만 특정 URL 패턴을 통해 접근을 허용합니다.
        String convertedPath = rootDir + "/" + FileType.CONVERTED.getDirectoryName() + "/";

        registry.addResourceHandler(convertedBaseUrl + "/**")
                .addResourceLocations("file:" + convertedPath);
    }
}
