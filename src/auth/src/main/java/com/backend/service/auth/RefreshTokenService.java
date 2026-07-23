package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.config.ApplicationPolicies;
import com.backend.config.ApplicationProperties;
import com.backend.dto.auth.refresh.RefreshResponse;
import com.backend.handler.AuthException;
import com.backend.handler.UserSuspendException;
import com.backend.model.RefreshToken;
import com.backend.repository.RefreshTokenRepository;
import com.backend.security.jwt.JwtTokenProvider;
import com.backend.security.userdetails.CustomUserDetails;
import com.backend.security.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshRep;
    private final ApplicationProperties appProperties;
    private final ApplicationPolicies appPolicies;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public String create(Long userId, String ipAddress, String userAgent, String deviceId) {

        checkDeviceIdAndPolicies(userId);

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .token(UUID.randomUUID().toString())
                .expirationDate(
                        LocalDateTime.now().plusSeconds(
                                appProperties.getJwt().getRefreshTokenExpirationSec()
                        )
                )
                .build();

        token = refreshRep.save(token);

        log.info("Refresh-token for User ID: {} made successfully.", userId);

        return token.getToken();
    }

    private void checkDeviceIdAndPolicies(Long userId) {
        if (!appPolicies.getAllowConcurrentLogins()) {
            int changed = refreshRep.deactivateByUser(userId, LocalDateTime.now(),
                    "Concurrent login while app policies didn't allow it");
            if (changed > 0) {
                log.info("{} refresh-token(s) of User ID {} deactivated (Concurrent logins).", changed, userId);
            }
        }
    }

    @Transactional(
            noRollbackFor = {
                    AuthException.class,
                    UserSuspendException.class
            }
    )
    public RefreshResponse refresh(String requestRefreshToken) {
        log.info("Attempting to rotate refresh token and generate new access token...");

        RefreshToken refreshToken = refreshRep.findByToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.warn("Failed refresh attempt: Refresh token not found in the database.");
                    return new AuthException(ApiMessage.REFRESH_TOKEN_NOT_EXIST);
                });

        // Reuse Detection
        if (!refreshToken.isActive()) {
            log.warn("SECURITY BREACH DETECTED: Attempted to reuse a REVOKED refresh token for user {}. Revoking all sessions...", refreshToken.getUserId());
            refreshRep.revokeAllTokensForUser(refreshToken.getUserId(), "Security Breach | Attempt to reuse a revoked token");
            throw new AuthException(ApiMessage.REFRESH_TOKEN_NOT_EXIST);
        }

        // Check Expiration
        if (refreshToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            refreshRep.revokeToken(refreshToken.getToken(), "Refresh-token expired before rotation");
            log.warn("Refresh token for user {} has expired. Token revoked.", refreshToken.getUserId());
            throw new AuthException(ApiMessage.REFRESH_TOKEN_EXPIRED);
        }

        // User Suspend Status
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(refreshToken.getUserId());
        if (!userDetails.isEnabled()) {
            refreshRep.revokeAllTokensForUser(userDetails.getId(), "User Banned | try to refresh access token");
            log.warn("Banned user {} try to refresh access token. All user tokens revoked.", refreshToken.getUserId());
            throw new UserSuspendException("You are banned", "banned");
        }

        // ====================================
        //              ROTATION
        // ====================================

        refreshRep.revokeToken(refreshToken.getToken(), "Consumed normally for token rotation");
        log.debug("Old refresh token for user {} consumed successfully.", refreshToken.getUserId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(refreshToken.getUserId())
                .ipAddress(refreshToken.getIpAddress())
                .userAgent(refreshToken.getUserAgent())
                .deviceId(refreshToken.getDeviceId())
                .token(UUID.randomUUID().toString())
                .expirationDate(
                        LocalDateTime.now().plusSeconds(
                                appProperties.getJwt().getRefreshTokenExpirationSec()
                        )
                )
                .build();

        newRefreshToken = refreshRep.save(newRefreshToken);


        String newAccessToken = jwtTokenProvider.generateToken(userDetails);

        log.info("Token rotation successful for user {}. New access and refresh tokens generated.", refreshToken.getUserId());

        return RefreshResponse.builder()
                .access_token(newAccessToken)
                .refresh_token(newRefreshToken.getToken()) // new one
                .build();
    }

    @Transactional
    public void revokeAllByUserId(Long userId, String reason) {
        log.info("Revoking all refresh tokens for user ID: {} due to password reset...", userId);
        refreshRep.revokeAllTokensForUser(userId, reason);
        log.info("All refresh tokens for user ID: {} successfully revoked.", userId);
    }

    @Transactional
    public void revokeRefreshToken(String requestRefreshToken) {
        log.info("Attempting to revoke refresh token...");

        refreshRep.findByToken(requestRefreshToken).ifPresentOrElse(
                token -> {
                    if (token.isActive()) {
                        refreshRep.revokeToken(token.getToken(), "User logged out manually");
                        log.info("Refresh token for user {} successfully revoked.", token.getUserId());
                    } else {
                        log.info("Refresh token is already revoked. Skipping.");
                    }
                },
                () -> log.warn("Refresh token not found.")
        );
    }
}