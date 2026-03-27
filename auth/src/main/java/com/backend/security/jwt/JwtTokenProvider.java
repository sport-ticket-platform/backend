package com.backend.security.jwt;

import com.backend.config.ApplicationProperties;
import com.backend.security.userdetails.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>JWT Token Provider (RS256 - Production Ready)</h2>
 *
 * <ul>
 *     <li>Explicit RS256</li>
 *     <li>Issuer & Audience validation</li>
 *     <li>Reusable JwtParser</li>
 * </ul>
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

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private JwtParser jwtParser;

    /**
     * Load RSA keys and initialize parser once at startup.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing JWT infrastructure(public and private kay)...");
        try {
            this.privateKey = loadPrivateKey(appPrp.getJwt().getPrivateKey());
            this.publicKey = loadPublicKey(appPrp.getJwt().getPublicKey());

            this.jwtParser = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(appPrp.getJwt().getIssuer())
                    .build();

            log.info("JWT RS256 infrastructure initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize JWT infrastructure. Server will refuse to start.", e);
            throw new IllegalStateException("Cannot start application without valid JWT keys", e);
        }
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Long userId = userDetails.getId();

        Date now = new Date();
        Date expiryDate = new Date(
                now.getTime() + appPrp.getJwt().getAccessTokenExpirationSec() * 1_000
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("roles", roles)
                .claim("username", userDetails.getUsername())
                .issuer(appPrp.getJwt().getIssuer())
                .audience().add(appPrp.getJwt().getAudience()).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
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
    private PrivateKey loadPrivateKey(Resource keyRes) throws Exception {
        String key = new String(keyRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Decoders.BASE64.decode(privateKeyPEM);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
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
