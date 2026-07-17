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

    public RefreshResponse refresh(String requestRefreshToken) {
        log.info("Attempting to refresh token...");

        RefreshToken refreshToken = refreshRep.findByToken(requestRefreshToken)
                .orElseThrow(() -> new AuthException(ApiMessage.REFRESH_TOKEN_NOT_EXIST));

        if (refreshToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token for user {} has expired. Token revoked.", refreshToken.getUserId());
            throw new AuthException(ApiMessage.REFRESH_TOKEN_EXPIRED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(refreshToken.getUserId());

        if (!userDetails.isEnabled()) {
            throw new UserSuspendException("You are banned", "banned");
        }

        String newAccessToken = jwtTokenProvider.generateToken(userDetails);

        log.info("New access token generated successfully for user {}", refreshToken.getUserId());

        return RefreshResponse.builder()
                .access_token(newAccessToken)
                .build();
    }
}