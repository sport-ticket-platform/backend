package com.backend.service;

import com.backend.config.ApplicationPolicies;
import com.backend.config.ApplicationProperties;
import com.backend.entity.BlacklistedIp;
import com.backend.entity.RefreshToken;
import com.backend.entity.User;
import com.backend.repository.RefreshTokenRepository;
import com.backend.service.system.BlacklistedIpService;
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

    private final BlacklistedIpService blackIpService;

    public String createRefreshToken(User user, String ipAddress, String userAgent, String deviceId) {

        checkEntries(userAgent, deviceId, user.getId());

        checkDeviceIdAndPolicies(user.getId(), deviceId);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        token.setDeviceId(deviceId);

        token.setToken(UUID.randomUUID().toString());
        token.setExpirationDate(
                LocalDateTime.now().plusSeconds(
                        appProperties.getJwt().getRefreshTokenExpirationSec()
                )
        );

        token = refreshRep.save(token);

        log.info("User {}(id: {}) refresh-token made successfully.", user.getUsername(), user.getId());

        return token.getToken();
    }

    private void checkDeviceIdAndPolicies(Long userId, String deviceId) {
        if (!appPolicies.getAllowConcurrentLogins()) {
            int changed = refreshRep.deactivateAllByUserId(userId, LocalDateTime.now(),
                    "Concurrent login while app policies didn't allow it");
            if (changed > 0) {
                log.info("{} refresh-token(s) of User {} deactivated (Concurrent logins).", changed, userId);
            }
        }
        {
            int changed = refreshRep.deactivateAllByDeviceId(deviceId, LocalDateTime.now(),
                    "New login with same device");
            if (changed > 0) {
                log.info("{} refresh-token(s) for device '{}' deactivated.", changed, deviceId);
            }
        }
    }

    private void checkEntries(String userAgent, String deviceId, Long userId) {
        if (userAgent.length() > 512) {
            log.warn("User {} Trying to insert a longer than 512-char string into userAgent. entry len: {}",
                    userId, userAgent.length());
            blackIpService.blockIpForever(
                    BlacklistedIp.builder().build()
            );
        } else if (deviceId.length() > 256) {

        }
    }
}
