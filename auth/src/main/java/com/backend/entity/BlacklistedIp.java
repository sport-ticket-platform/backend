package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * <h2>Entity representing a permanently blocked IP address</h2>
 * <p>
 * This table stores the absolute worst offenders who decided to test the limits
 * of our application. <b>Do not</b> use this for temporary rate-limiting, as that
 * is handled purely in-memory.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Entity
@Table(name = "blacklisted_ip", indexes = {
        @Index(name = "idx_ip_address", columnList = "ip_address"),
        @Index(name = "idx_culprit_user", columnList = "culprit_user_id"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column
    private String reason;

    /**
     * The identifier of the authenticated user at the time of the malicious activity.
     * <p>
     * <mark>Note:</mark> This is strictly for auditing purposes. Do not cascade bans
     * based solely on this field due to the nature of shared IP networks (NAT).
     * </p>
     */
    @Column
    private Long culpritUserId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime blockedAt;

    /**
     * Soft delete flag to temporarily forgive an IP without losing its historical record.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void prePersist() {
        this.blockedAt = LocalDateTime.now();
    }
}
