package com.reservation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Security security = new Security();

    @Data
    public static class Security {
        private Jwt jwt = new Jwt();
        private List<String> publicPaths;
    }

    @Data
    public static class Jwt {
        private Resource publicKey;
        private String issuer;
    }
}