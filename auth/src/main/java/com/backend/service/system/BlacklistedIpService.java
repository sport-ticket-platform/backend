package com.backend.service.system;

import com.backend.config.RedisKeyNames;
import com.backend.entity.BlacklistedIp;
import com.backend.repository.BlacklistedIpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for synchronizing the persistent IP blacklist
 * from the relational database to the in-memory Redis cache.
 * <p>
 * Utilizes a paginated approach to prevent high memory consumption when
 * the total number of blacklisted IPs ($N$) is significantly large.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BlacklistedIpService {

    private final BlacklistedIpRepository blackIpRepository;
    private final StringRedisTemplate redis;

    private static final String BLACKLISTED_IPS_KEY = RedisKeyNames.BLACKLISTED_IPS_KEY;
    private static final String IPS_ILLEGALS_COUNT = RedisKeyNames.IPS_ILLEGALS_COUNT;
    private static final int MAXIMUM_ILLEGALS_COUNT = 20;
    private static final int BATCH_SIZE = 500;

    /**
     * Executes the synchronization logic.
     * <br>
     * <b>Triggers:</b>
     * <ul>
     *     <li>Automatically upon application startup (when the context is fully ready).</li>
     *     <li>Daily at 03:30 AM server time via a CRON job.</li>
     * </ul>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 30 3 * * ?")
    @Transactional(readOnly = true)
    public void syncDatabaseToRedis() {
        log.info("Starting synchronization of blacklisted IPs to Redis...");

        redis.delete(BLACKLISTED_IPS_KEY);

        int pageNumber = 0;
        Page<BlacklistedIp> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            page = blackIpRepository.findAllByIsActiveTrue(pageable);

            List<String> ips = page.getContent().stream()
                    .map(BlacklistedIp::getIpAddress)
                    .toList();

            if (!ips.isEmpty()) {
                redis.opsForSet().add(BLACKLISTED_IPS_KEY, ips.toArray(new String[0]));
                log.info("Synced a batch of {} IPs to Redis. Current page: {}", ips.size(), pageNumber);
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("Synchronization of blacklisted IPs to Redis completed successfully.");
    }

    public void blockIpForever(BlacklistedIp bp) {
        String ip = bp.getIpAddress();
        log.warn("Ip {} is blocked forever. user: {}", ip, bp.getCulpritUserId());

        Boolean exists = redis.opsForSet().isMember(BLACKLISTED_IPS_KEY, ip);

        if (Boolean.FALSE.equals(exists)) {
            redis.opsForSet().add(BLACKLISTED_IPS_KEY, ip);
            log.info("Ip {} added to redis blacklist.", ip);
        } else {
            log.error("The user’s IP {} was already present in Redis," +
                    " while it should have been blocked earlier " +
                    "to prevent it from reaching this layer of the application.", ip);
            throw new IllegalStateException("An ip " + ip + " that is blocked was able to access");
        }

        redis.opsForSet().add(BLACKLISTED_IPS_KEY, bp.getIpAddress());

        blackIpRepository.save(bp);
    }

    public void addIllegalActivity(int penalty, BlacklistedIp bp) {
        log.warn("{} Penalty added for '{}' ip", penalty, bp.getIpAddress());
    }
}
