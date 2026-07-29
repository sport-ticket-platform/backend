package com.reservation.service.grpc;

import com.reservation.dto.grpc.ReservedSeatDTO;
import com.reservation.grpc.GetReservedSeatsByConfigIdsRequest;
import com.reservation.grpc.GetReservedSeatsByConfigIdsResponse;
import com.reservation.grpc.ReservationServiceGrpc;
import com.reservation.grpc.ReservedSeat;
import com.reservation.repository.ReservationRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ReservationGrpcService extends ReservationServiceGrpc.ReservationServiceImplBase {

    private final ReservationRepository reservationRepository;

    @Override
    public void getReservedSeatsByConfigIds(GetReservedSeatsByConfigIdsRequest request,
                                            StreamObserver<GetReservedSeatsByConfigIdsResponse> responseObserver) {

        List<Integer> configIdsList = request.getConfigIdsList();

        try {
            log.info("Received request to fetch reserved seats for configIds: {}", configIdsList);

            List<ReservedSeatDTO> reservedSeatDTOS =
                    reservationRepository.getReservedSeats(configIdsList);

            List<ReservedSeat> grpcReservedSeats = reservedSeatDTOS.stream()
                    .map(seat -> ReservedSeat.newBuilder()
                            .setSeatId(seat.seatId())
                            .setConfigId(seat.configId())
                            .build())
                    .toList();

            GetReservedSeatsByConfigIdsResponse response = GetReservedSeatsByConfigIdsResponse.newBuilder()
                    .addAllReservedSeats(grpcReservedSeats)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully fetched {} reserved/sold seats for configIds: {}", grpcReservedSeats.size(), configIdsList);

        } catch (Exception e) {
            log.error("Error occurred while fetching reserved seats for configIds: {}", configIdsList, e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error while fetching reserved seats")
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}