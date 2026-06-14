package com.backend.repository;

import com.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Boolean existsByDeviceIdAndIsActiveTrue(String deviceId);

    Boolean existsByUserIdAndIsActiveTrue(Long userId);

    @Modifying
    @Transactional
    @Query("""
       UPDATE RefreshToken t
       SET t.isActive = false,
           t.revokedAt = :revokedAt,
           t.revokedReason = :revokedReason
       WHERE t.user.id = :userId
       """)
    int deactivateAllByUserId(
            @Param("user_id") Long userId,
            @Param("revokedAt")LocalDateTime time,
            @Param("revokedReason") String reason
    );

    @Modifying
    @Transactional
    @Query("""
       UPDATE RefreshToken t
       SET t.isActive = false,
           t.revokedAt = :revokedAt,
           t.revokedReason = :revokedReason
       WHERE t.deviceId = :deviceId AND t.isActive = true
       """)
    int deactivateAllByDeviceId(
            @Param("deviceId") String deviceId,
            @Param("revokedAt")LocalDateTime time,
            @Param("revokedReason") String reason
    );
}
