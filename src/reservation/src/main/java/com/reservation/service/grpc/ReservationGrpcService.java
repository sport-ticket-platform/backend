package com.reservation.service.grpc;

import com.reservation.grpc.GetReservedSeatsByConfigIdsRequest;
import com.reservation.grpc.GetReservedSeatsByConfigIdsResponse;
import com.reservation.grpc.ReservationServiceGrpc;
import com.reservation.grpc.ReservedSeat;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
public class ReservationGrpcService extends ReservationServiceGrpc.ReservationServiceImplBase {

    @Override
    public void getReservedSeatsByConfigIds(GetReservedSeatsByConfigIdsRequest request,
                                            StreamObserver<GetReservedSeatsByConfigIdsResponse> responseObserver) {

        log.info("{}", request.getConfigIdsList());
        List<ReservedSeat> mockReservedSeats = List.of(
                ReservedSeat.newBuilder().setSeatId(101L).setConfigId(1).build(),
                ReservedSeat.newBuilder().setSeatId(102L).setConfigId(1).build(),
                ReservedSeat.newBuilder().setSeatId(201L).setConfigId(2).build()
        );

        GetReservedSeatsByConfigIdsResponse response = GetReservedSeatsByConfigIdsResponse.newBuilder()
                .addAllReservedSeats(mockReservedSeats)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}