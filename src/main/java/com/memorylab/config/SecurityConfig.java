package com.memorylab.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ JWT 기반이므로 전역 CSRF 비활성화 (로그인/회원가입 403 방지)
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1) 정적/공개 페이지
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/img/**","/thumbnails/**","/images/**","/css/**","/js/**").permitAll()
                        .requestMatchers("/", "/index", "/login", "/signup", "/profile",
                                "/board/**", "/error", "/favicon.ico").permitAll()

                        // 2) CORS 프리플라이트
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3) ✅ 콜백(Webhook) 허용 (순서: /api/** 인증 규칙보다 위에 둔다)
                        .requestMatchers(HttpMethod.GET,  "/api/v1/ai-callback/notify/ping").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ai-callback/notify").permitAll()

                        // 4) ✅ 인증/회원가입 공개 API
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/send-verification-code",
                                "/api/auth/verify-code"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/check-email",
                                "/api/auth/check-nickname"
                        ).permitAll()

                        // 5) 게시판 목록/상세 공개
                        .requestMatchers(HttpMethod.GET, "/api/board", "/api/board/**").permitAll()

                        // 5-1) Admin API 임시 허용 (TODO: ADMIN 역할 체크 추가 필요)
                        .requestMatchers("/api/admin/**").permitAll()

                        // 6) 그 외 /api/** 는 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // 7) 나머지는 인증
                        .anyRequest().authenticated()
                )
                // JWT 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 운영 시 실제 도메인만 허용
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://mlab.snowytiger.me",
                "https://*.memorylab.com",
                "https://localhost:*",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
