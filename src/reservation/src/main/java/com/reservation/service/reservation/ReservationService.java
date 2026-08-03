package com.reservation.service.reservation;

import com.reservation.common.ApiMessage;
import com.reservation.config.ApplicationProperties;
import com.reservation.dto.PageResult;
import com.reservation.dto.reservation.ReservationRequest;
import com.reservation.dto.reservation.ReservationResponse;
import com.reservation.dto.reservation.get.ReservationDetailResponse;
import com.reservation.dto.reservation.get.ReservesHistoryRequest;
import com.reservation.handler.BusinessException;
import com.reservation.model.Reservation;
import com.reservation.model.ReservationSeat;
import com.reservation.model.ReservationStatus;
import com.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provides business logic for Reservation.
 *
 * @author logTAHA
 * @since 1.0.0
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepo;
    private final ApplicationProperties appProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * Creates a new reservation for the specified seats.
     *
     * <p>Constraints:
     * <ul>
     *   <li>The seat IDs list must not contain duplicate values.</li>
     *   <li>The seat IDs list must contain at most 15 items.</li>
     * </ul>
     *
     * @param userId the ID of the user creating the reservation
     * @param request the reservation request containing the selected seat IDs
     * @return the created reservation
     *
     * @author logTAHA
     */
    @Transactional
    public ReservationResponse reserve(Long userId, ReservationRequest request) {

        log.info("Reserving {} seats...", request.seat_ids().size());

        // ============================
        //         Validating
        // ============================
        validateSeatsForReserving(request);

        // ============================
        //         Reserving
        // ============================
        List<ReservationRepository.SeatConfigInfo> seatConfigInfos = reservationRepo.findAllSeatConfigInfo(request.seat_ids());
        if (seatConfigInfos.isEmpty()) {
            log.error("No seat config info found for seat IDs: {}", request.seat_ids());
            throw new IllegalStateException("Failed to load configuration and pricing info for seats: " + request.seat_ids());
        }
        BigDecimal totalPrice = seatConfigInfos.stream()
                .map(ReservationRepository.SeatConfigInfo::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Total amount calculated: {} for {} seats", totalPrice, seatConfigInfos.size());

        long activeTimeSec = appProperties.getBusiness().getReservationActiveTimeSec();
        OffsetDateTime expirationTime = OffsetDateTime.now().plusSeconds(activeTimeSec);

        Long reservationId = reservationRepo.createReservation(userId, expirationTime);
        log.info("Successfully created reservation [ID: {}] for user [ID: {}]", reservationId, userId);

        // set timer for release seats if user didn't pay
        scheduleReservationExpiration(reservationId, activeTimeSec);

        reservationRepo.insertReservationSeats(reservationId, seatConfigInfos);
        log.info("{} seats successfully reserved for reservation ID: {}", seatConfigInfos.size(), reservationId);

        Long orderId = reservationRepo.createTicketOrder(reservationId, userId, totalPrice);
        log.info("Ticket order successfully created [Order ID: {}] for [Reservation ID: {}]", orderId, reservationId);

        return ReservationResponse.builder()
                .reservationId(reservationId)
                .orderId(orderId)
                .expiresAt(expirationTime)
                .build();
    }

    /**
     * Sets a key in Redis with a TTL. When this key expires, Redis will publish an expiration event.
     * The listener will catch this event and trigger the expiration process in the database.
     */
    private void scheduleReservationExpiration(Long reservationId, long ttlSeconds) {
        String redisKey = "reservation:expire:" + reservationId;
        redisTemplate.opsForValue().set(redisKey, "PENDING", Duration.ofSeconds(ttlSeconds));
        log.info("Scheduled expiration alarm in Redis for reservation ID: {} with TTL: {} seconds", reservationId, ttlSeconds);
    }

    /**
     * <h6>Expire an active reservation</h6>
     * <ul>
     *   <li>reservation table status: EXPIRED</li>
     *   <li>reservation_seat, each one: is_active = false</li>
     *   <li>ticket_order, status: FAILED</li>
     * </ul>
     */
    @Transactional
    public void expireReservation(Long reservationId) {
        log.info("Starting expiration process for reservation ID: {}", reservationId);

        try {
            reservationRepo.expireReservation(reservationId);
            log.info("Successfully expired reservation ID: {} (Seats released, order failed).", reservationId);
        } catch (Exception e) {
            log.error("Failed to expire reservation ID: {}. Triggering rollback.", reservationId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PageResult<Reservation> getUserReservationHistory(Long userId, ReservesHistoryRequest request) {
        log.debug("Processing reservation history fetch for userId: {}, page: {}, pageSize: {}, status: {}",
                userId, request.page(), request.page_size(), request.status());

        return reservationRepo.getUserReserveHistory(
                userId,
                request.page(),
                request.page_size(),
                request.status()
        );
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationById(Long userId, Long reservationId) {
        log.info("Fetching reservation details for reservationId: {} and userId: {}", reservationId, userId);

        Reservation reservation = reservationRepo.findUserReservationById(reservationId, userId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found or access denied. reservationId: {}, userId: {}", reservationId, userId);
                    return new BusinessException(ApiMessage.RESOURCE_NOT_FOUND);
                });

        Long orderId = reservationRepo.findOrderIdByReservationId(reservationId, userId).orElse(null);
        if (orderId == null) {
            log.warn("Order not found or access denied for reservationId: {} and userId: {}", reservationId, userId);
        }

        if (ReservationStatus.COMPLETED.equals(reservation.getStatus())) {
            log.info("Reservation {} is COMPLETED. Redirecting to order details.", reservationId);

            return ReservationDetailResponse.builder()
                    .reservation(reservation)
                    .orderId(orderId)
                    .build();
        }

        // If not 'COMPLETED'
        List<com.reservation.model.ReservationSeat> seats = reservationRepo.findUserReservationSeatsDetails(reservationId);

        Long matchId = null;
        if (seats != null && !seats.isEmpty()) {
            com.reservation.model.TicketConfig config = seats.getFirst().getTicketConfig();
            if (config != null) {
                matchId = config.getMatchId();
            }
        }

        return ReservationDetailResponse.builder()
                .reservation(reservation)
                .orderId(orderId)
                .matchId(matchId)
                .reservationSeats(seats)
                .build();
    }

    private void checkSeatIdsList(List<Long> seatIds) {
        if (seatIds.isEmpty() || seatIds.size() > 15) {
            log.warn("Seats list is empty or more than 15.");
            throw new BusinessException(ApiMessage.RESERVE_SEAT_IDS_SIZE);
        }

        Set<Long> set = new HashSet<>();
        Set<Long> duplicates =  new HashSet<>();
        int totalDuplicateCount = 0;
        for (Long id : seatIds) {
            if (set.contains(id)) {
                duplicates.add(id);
                totalDuplicateCount++;
            }

            set.add(id);
        }

        if (!duplicates.isEmpty()) {
            record DuplicateSeat(Set<Long> duplicates) {}
            log.warn("[total duplicates: {} | unique duplicates: {}] Duplicate seats found in user reservation list.", totalDuplicateCount, duplicates.size());
            throw new BusinessException(
                    ApiMessage.RESERVE_SEAT_IDS_REPEATED,
                    new  DuplicateSeat(duplicates)
            );
        }
    }

    private void validateSeatsForReserving(ReservationRequest request) {
        // check seat list size (1 till 15) and no duplicate in that
        checkSeatIdsList(request.seat_ids());

        // check all seats exist
        List<Long> nonExistingSeats = reservationRepo.findNonExistSeatIds(request.seat_ids());
        if (!nonExistingSeats.isEmpty()) {
            record SeatsNotExist(List<Long> non_existing) {}
            log.warn("{} seats not exist", nonExistingSeats.size());
            throw new BusinessException(
                    ApiMessage.SEATS_NOT_EXIST,
                    new SeatsNotExist(nonExistingSeats)
            );
        }

        // check all seats are from same match
        Optional<Long> matchId = reservationRepo.findSingleMatchIdForSeats(request.seat_ids());
        if (matchId.isEmpty()) {
            log.warn("All seats not belong to the same match");
            throw new BusinessException(ApiMessage.SEATS_NOT_FOR_ONE_MATCH);
        }

        // check match available and payment is open
        ReservationRepository.MatchStatus matchStatus = reservationRepo.getMatchStatus(matchId.get())
                .orElseThrow(() -> {
                    log.error("No match found for matchId {}", matchId.get());
                    return new IllegalStateException("No match found for matchId: " + matchId.get());
                });
        if (!matchStatus.isAvailable()) {
            log.warn("Match [id: {}] is not available for reserving ticket", matchId.get());
            throw new BusinessException(ApiMessage.MATCH_NOT_AVAILABLE);
        }
        if (!matchStatus.isPaymentOpen()) {
            log.warn("Match [id: {}] payment is not open for reserving ticket", matchId.get());
            throw new BusinessException(ApiMessage.MATCH_PAYMENT_IS_NOT_OPEN);
        }

        // check all seats available(not reserved or sold)
        List<Long> nonAvailableSeats = reservationRepo.findNotAvailableSeatIds(request.seat_ids());
        if (!nonAvailableSeats.isEmpty()) {
            record SeatsNotAvailable(List<Long> non_available) {}
            log.warn("{} seats not available", nonAvailableSeats.size());
            throw new BusinessException(
                    ApiMessage.SEATS_NOT_AVAILABLE,
                    new SeatsNotAvailable(nonAvailableSeats)
            );
        }
    }
}