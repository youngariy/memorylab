package com.memorylab.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcLoggingFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            // 요청에 대한 고유한 traceId 생성
            String traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID_KEY, traceId);

            // 응답 헤더에도 traceId 추가
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).addHeader(TRACE_ID_HEADER, traceId);
            }

            chain.doFilter(request, response);
        } finally {
            // 요청 처리가 끝나면 MDC에서 traceId 제거
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
