package com.memorylab.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey jwtSecretKey;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);

        // [A] 시작 로깅: 헤더 유무/토큰 길이
        logger.info("JWT START path={}, hasAuthHeader={}, tokenLen={}",
                request.getServletPath(),
                request.getHeader("Authorization") != null,
                token != null ? token.length() : 0);

        if (token != null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(jwtSecretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // [B] 파싱 성공 로깅: sub/roles
                logger.info("JWT OK sub={}, roles={}", claims.getSubject(), claims.get("roles"));

                String email = claims.getSubject();
                if (email != null) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    if (roles == null) {
                        roles = List.of();
                    }

                    var authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                    UserDetails userDetails = new User(email, "", authorities);

                    var auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // [C] 인증 객체 세팅 로깅
                    logger.info("JWT AUTH SET principal={}, authorities={}", email, authorities);
                }
            } catch (JwtException e) {
                logger.warn("JWT parse/verify failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return null;
        }
        return bearer.substring(7);
    }
}
