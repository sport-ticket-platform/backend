package com.backend.repository;

import com.backend.entity.BlacklistedIp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * <h2>Repository interface for managing blacklisted IP entities</h2>
 * <p>
 * Provides data access operations for querying active bans and maintaining
 * historical records of malicious network actors.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
public interface BlacklistedIpRepository extends JpaRepository<BlacklistedIp, Long> {

    boolean existsByIpAddressAndIsActiveTrue(String ipAddress);

    Optional<BlacklistedIp> findFirstByIpAddressAndIsActiveTrueOrderByBlockedAtDesc(String ipAddress);

    Page<BlacklistedIp> findAllByIsActiveTrue(Pageable pageable);

    @Modifying
    @Query("UPDATE BlacklistedIp b SET b.isActive = false WHERE b.ipAddress = :ipAddress AND b.isActive = true")
    void deactivateActiveBansForIp(@Param("ipAddress") String ipAddress);
}
