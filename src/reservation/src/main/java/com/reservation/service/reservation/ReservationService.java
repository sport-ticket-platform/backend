package com.reservation.service.reservation;

import com.reservation.common.ApiMessage;
import com.reservation.dto.reservation.ReservationRequest;
import com.reservation.dto.reservation.ReservationResponse;
import com.reservation.handler.BusinessException;
import com.reservation.repository.ReservationRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        ReservationRepository.MatchStatus matchStatus = reservationRepo.getMatchStatus(matchId.get());
        if (matchStatus == null) {
            log.error("No match found for matchId {}", matchId.get());
            throw new IllegalStateException("No match found for matchId: " + matchId.get());
        } else if (!matchStatus.isAvailable()) {
            log.warn("Match [id: {}] is not available for reserving ticket", matchId.get());
            throw new BusinessException(ApiMessage.MATCH_NOT_AVAILABLE);
        } else if (!matchStatus.isPaymentOpen()) {
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

        // ۳. ایجاد رکورد اصلی رزرو (در جدول reservation) و محاسبه زمان انقضا (مثلاً ۱۵ دقیقه)

        // ۴. ثبت صندلی‌های انتخاب‌شده به نام این رزرو (در جدول reservation_seat)

        // ۵. ساخت و برگرداندن DTO پاسخ (ReservationResponse)

        return null; // فعلاً برای کامپایل شدن کد
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
}