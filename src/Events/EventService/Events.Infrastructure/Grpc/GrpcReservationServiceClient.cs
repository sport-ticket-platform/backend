using EventService.Events.Application.Interfaces;
using EventService.Events.Application.Queries.GetSeatsByConfig;
using EventService.Events.Infrastructure.Exceptions;
using EventService.Reservations.Grpc;
using Grpc.Core;

namespace EventService.Events.Infrastructure.Grpc;

public class GrpcReservationServiceClient : IReservationServiceClient
{
    private readonly ReservationService.ReservationServiceClient _grpcClient;
    private readonly ILogger<GrpcReservationServiceClient> _logger;

    public GrpcReservationServiceClient(ReservationService.ReservationServiceClient grpcClient,
        ILogger<GrpcReservationServiceClient> logger)
    {
        _grpcClient = grpcClient;
        _logger = logger;
    }

    public async Task<IEnumerable<ReservedSeatDto>> GetReservedSeatsByConfigIdsAsync(
        IEnumerable<int> configIds,
        CancellationToken cancellationToken)
    {
        var request = new GetReservedSeatsByConfigIdsRequest();
        request.ConfigIds.AddRange(configIds);

        _logger.LogInformation(
            "Requesting reserved seats from reservation service for {Count} config(s)",
            request.ConfigIds.Count);

        try
        {
            var response = await _grpcClient.GetReservedSeatsByConfigIdsAsync(
                request, cancellationToken: cancellationToken);

            return response.ReservedSeats.Select(rs => new ReservedSeatDto
            {
                SeatId = rs.SeatId,
                ConfigId = rs.ConfigId
            });
        }
        catch (RpcException ex)
        {
            _logger.LogError(ex,
                "gRPC call to reservation service failed for config IDs [{ConfigIds}]",
                string.Join(", ", configIds));
            throw new InfrastructureException("Failed to retrieve reserved seat data from the reservation service.");
        }
    }
}