package com.reservation.service.listener;

import com.reservation.service.reservation.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReservationExpirationService extends KeyExpirationEventMessageListener {

    public static final String EXPIRE_KEY_PREFIX = "reservation:expire:";

    private final ReservationService reservationService;

    public ReservationExpirationService(RedisMessageListenerContainer listenerContainer,  ReservationService reservationService) {
        super(listenerContainer);
        this.reservationService = reservationService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith(EXPIRE_KEY_PREFIX)) {
            try {
                String[] parts = expiredKey.split(":");
                String idStr = parts[parts.length - 1];
                Long reservationId = Long.parseLong(idStr);

                log.info("Reservation {} canceled due to payment timeout (Redis TTL expired).", reservationId);

                reservationService.expireReservation(reservationId);

            } catch (NumberFormatException e) {
                log.error("Invalid reservation ID format in Redis key: {}", expiredKey, e);
            } catch (Exception e) {
                log.error("Error processing expired reservation key: {}", expiredKey, e);
            }
        }
    }
}