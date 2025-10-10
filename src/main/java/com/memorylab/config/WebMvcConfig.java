package com.memorylab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.root-dir}")
    private String uploadDir;

    @Value("${app.upload.converted-base-url}")
    private String resourceHandler;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(resourceHandler + "/**")
                .addResourceLocations("file:" + uploadDir);
    }

    /**
     * SPA Fallback Configuration (Spring Boot 3 Compatible)
     *
     * Note: Spring Boot 3 uses PathPatternParser which does not support
     * regex patterns after double-asterisk (invalid pattern removed).
     *
     * For SPA fallback, we handle it via:
     * 1. ErrorController (404 -> index.html) - see SpaFallbackController
     * 2. Or use a custom controller for specific paths
     *
     * This simple pattern handles root-level paths only.
     * Nested paths (e.g., /post/123) are handled by SpaFallbackController.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Handle root and single-level paths (e.g., /login, /register)
        // Pattern: matches paths without dots (excludes static files like .js, .css)
        registry.addViewController("/{path:[^.]*}")
                .setViewName("forward:/index.html");
    }
}
