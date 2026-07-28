package com.reservation.security.jwt;

import com.reservation.config.ApplicationProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * <h2>JWT Token Provider (Verification Only - RS256)</h2>
 * <p>Used in downstream microservices to validate JWT tokens using the Public Key.</p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final ApplicationProperties appPrp;

    private PublicKey publicKey;
    private JwtParser jwtParser;

    /**
     * Load RSA public key and initialize parser once at startup.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing JWT Verification infrastructure (Public Key only)...");
        try {
            this.publicKey = loadPublicKey(appPrp.getSecurity().getJwt().getPublicKey());

            this.jwtParser = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(appPrp.getSecurity().getJwt().getIssuer())
                    .build();

            log.info("JWT RS256 Verification infrastructure initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize JWT infrastructure. Server will refuse to start.", e);
            throw new IllegalStateException("Cannot start application without valid JWT public key", e);
        }
    }

    public Claims validateAndGetClaims(String token) {
        try {
            return jwtParser
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (SecurityException | MalformedJwtException e) {
            throw new InvalidJwtAuthException("Invalid JWT signature", e);
        } catch (ExpiredJwtException e) {
            throw new InvalidJwtAuthException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            throw new InvalidJwtAuthException("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            throw new InvalidJwtAuthException("JWT token is empty or invalid", e);
        }
    }

    // ===================== KEY LOADING =====================
    private PublicKey loadPublicKey(Resource keyRes) throws Exception {
        String key = new String(keyRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String publicKeyPEM = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Decoders.BASE64.decode(publicKeyPEM);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(encoded));
    }
}