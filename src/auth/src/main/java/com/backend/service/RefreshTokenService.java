package com.backend.service;

import com.backend.config.ApplicationPolicies;
import com.backend.config.ApplicationProperties;
import com.backend.model.RefreshToken;
import com.backend.repository.RefreshTokenRepository;
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

    public String createRefreshToken(Long userId, String ipAddress, String userAgent, String deviceId) {

        checkDeviceIdAndPolicies(userId, deviceId);

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

    private void checkDeviceIdAndPolicies(Long userId, String deviceId) {
        if (!appPolicies.getAllowConcurrentLogins()) {
            int changed = refreshRep.deactivateByUser(userId, LocalDateTime.now(),
                    "Concurrent login while app policies didn't allow it");
            if (changed > 0) {
                log.info("{} refresh-token(s) of User ID {} deactivated (Concurrent logins).", changed, userId);
            }
        }

        int changed = refreshRep.deactivateByDevice(deviceId, LocalDateTime.now(),
                "New login with same device");
        if (changed > 0) {
            log.info("{} refresh-token(s) for device '{}' deactivated.", changed, deviceId);
        }
    }
}