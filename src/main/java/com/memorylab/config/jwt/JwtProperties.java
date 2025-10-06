package com.memorylab.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 발급자를 정의합니다.
     */
    private String issuer = "memorylab";

    /**
     * JWT 대상자를 정의합니다. (예: memorylab-web)
     */
    private String audience = "memorylab-web";

    /**
     * 액세스 토큰의 유효 기간(분)입니다.
     */
    private int accessTokenMinutes = 60;

    /**
     * 리프레시 토큰의 유효 기간(일)입니다.
     */
    private int refreshTokenDays = 7;
}
