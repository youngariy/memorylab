package com.memorylab.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey jwtSecretKey; // JwtConfig의 @Bean 주입

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(jwtSecretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long userId = claims.get("uid", Long.class);

                if (userId != null) {
                    // === 토큰에서 roles 클레임 추출 ===
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    if (roles == null) {
                        roles = List.of(); // roles 클레임이 없는 경우를 대비한 방어 코드
                    }

                    // === 권한 목록을 SimpleGrantedAuthority로 변환 ===
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // === 동적으로 생성된 권한으로 인증 토큰 생성 ===
                    var auth = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities // 하드코딩된 권한 대신 토큰에서 추출한 권한 사용
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                // 유효하지 않거나 만료된 토큰 -> 401 Unauthorized 반환
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\": \"The access token is invalid or expired.\"}");
                return; // 필터 체인 중단
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null) return null;
        if (!bearer.startsWith("Bearer ")) return null;

        String t = bearer.substring(7).trim();
        // "Bearer"만 왔거나 "Bearer null" 같은 경우 방지
        if (t.isEmpty() || "null".equalsIgnoreCase(t) || "undefined".equalsIgnoreCase(t)) {
            return null;
        }
        return t;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 1) CORS 프리플라이트는 무조건 패스
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // 2) SSR/정적 페이지는 패스
        if (uri.equals("/")
                || uri.startsWith("/login")
                || uri.startsWith("/signup")
                || uri.startsWith("/profile")
                || uri.startsWith("/board")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.equals("/favicon.ico")
                || uri.startsWith("/error")) {
            return true;
        }

        // 3) 공개 인증 API만 패스
        if (uri.equals("/api/auth/login")
                || uri.equals("/api/auth/register")
                || uri.equals("/api/auth/verify")
                || uri.equals("/api/auth/refresh")) { // 재발급 경로 추가
            return true;
        }

        // 그 외는 필터 적용
        return false;
    }
}
