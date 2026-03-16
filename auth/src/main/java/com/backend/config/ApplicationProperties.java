package com.backend.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class ApplicationProperties {
    private final Jwt jwt = new Jwt();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationSec;
        private long refreshTokenExpirationSec;
    }
}
