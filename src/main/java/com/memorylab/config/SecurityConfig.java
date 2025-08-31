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

// (선택) JWT 필터가 있다면 주입하세요.
// import com.memorylab.security.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // 선택
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스(/**/css, js, images 등) 전부 허용
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // 서버 렌더링 페이지(컨트롤러 매핑) 전부 공개
                        .requestMatchers("/", "/index", "/login", "/signup", "/board/**", "/error", "/favicon.ico").permitAll()

                        // CORS 프리플라이트
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 API(로그인/회원가입 등) 공개
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/signup").permitAll()

                        // 게시판 REST: 목록/상세는 공개, 나머지 메서드는 인증
                        .requestMatchers(HttpMethod.GET, "/api/board", "/api/board/**").permitAll()

                        // 이 밖의 모든 /api/** 는 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // 혹시 남는 나머지 경로도 인증
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
}