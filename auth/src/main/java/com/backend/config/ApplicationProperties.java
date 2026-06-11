package com.backend.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Getter
@Setter
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class ApplicationProperties {

    private String[] publicPaths;
    private final Jwt jwt = new Jwt();
    private final EndpointLimitsPerMin endpointLimitsPerMin = new EndpointLimitsPerMin();

    /**
     * <h3>Use RSA(RS256) Algorithm</h3>
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Jwt {
        private Resource privateKey;
        private Resource publicKey;
        private long accessTokenExpirationSec;
        private long refreshTokenExpirationSec;
        private String issuer;
        private String audience;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EndpointLimitsPerMin {
        private int checkUsername;
        private int signup;
    }
}