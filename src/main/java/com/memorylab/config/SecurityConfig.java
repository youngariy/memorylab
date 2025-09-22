package com.memorylab.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.web.cors.*;

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
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // This will use the corsConfigurationSource bean below
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 허용
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/img/**", "/css/**", "/js/**").permitAll()

                        // 서버 렌더링 페이지 공개
                        .requestMatchers("/", "/index", "/login", "/signup", "/profile",
                                "/board/**", "/error", "/favicon.ico").permitAll()

                        // CORS 프리플라이트
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 관련 API 공개 설정
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

                        // 게시판 목록/상세 공개
                        .requestMatchers(HttpMethod.GET, "/api/board", "/api/board/**").permitAll()

                        // 이 밖의 모든 /api/** 는 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // 나머지는 전부 인증
                        .anyRequest().authenticated()
                )
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

        // 운영 출처는 Nginx에서 동일 출처로 처리되므로 제거.
        // 오직 로컬 개발 환경에서의 테스트만을 위해 허용합니다.
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:5173",    // 예: Vite 개발 서버
                "http://localhost:3000",    // 예: React 개발 서버
                "http://127.0.0.1:5500"   // 예: VSCode Live Server
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
