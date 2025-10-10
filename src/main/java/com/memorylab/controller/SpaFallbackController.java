package com.memorylab.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA Fallback Controller for React Router
 *
 * Handles 404 errors by forwarding to index.html, enabling:
 * - Deep linking (e.g., /post/123)
 * - Page refresh on any React route
 * - Browser back/forward navigation
 *
 * Spring Boot 3 Compatible: Uses PathPatternParser, no regex patterns needed.
 */
@Controller
public class SpaFallbackController implements ErrorController {

    /**
     * Handle all error requests
     *
     * For 404 errors on non-API routes → forward to index.html (SPA fallback)
     * For API errors → return error as-is (handled by @RestControllerAdvice)
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Only handle 404 Not Found errors
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

                // Don't intercept API errors - let REST exception handler deal with them
                if (requestUri != null && requestUri.startsWith("/api/")) {
                    return "forward:/error"; // Let default error handling work for API
                }

                // For non-API 404s, forward to index.html for React Router
                return "forward:/index.html";
            }
        }

        // For all other errors (500, etc.), use default error handling
        return "forward:/error";
    }
}
