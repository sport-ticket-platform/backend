package com.reservation.config;

import com.reservation.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once on application startup to expire any reservations that became
 * stale while the service was offline (e.g., during a restart or crash).
 *
 * <p>During normal operation, Redis key-expiration events handled by
 * {@link com.reservation.service.listener.ReservationExpirationService}
 * keep reservations up to date. However, those events are lost whenever the
 * application is not running. This runner closes that gap by performing a
 * one-time bulk sweep of the database immediately after the Spring context
 * is fully initialized.
 *
 * <p>Failures are caught and logged rather than re-thrown so that a sweep
 * error never prevents the application from starting.
 *
 * @author logTAHA
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupReservationSweepRunner implements ApplicationRunner {

    private final ReservationService reservationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== [Startup Sweep] Checking for stale ACTIVE reservations... ===");
        try {
            int expired = reservationService.expireAllStaleReservations();
            log.info("=== [Startup Sweep] Complete. {} reservation(s) expired. ===", expired);
        } catch (Exception e) {
            log.error("=== [Startup Sweep] Failed to expire stale reservations. App startup will continue. ===", e);
        }
    }
}
