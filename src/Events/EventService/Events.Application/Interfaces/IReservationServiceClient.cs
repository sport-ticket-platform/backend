using EventService.Events.Application.Queries.GetSeatsByConfig;

namespace EventService.Events.Application.Interfaces;

public interface IReservationServiceClient
{
    Task<IEnumerable<ReservedSeatDto>> GetReservedSeatsByConfigIdsAsync(
        IEnumerable<int> configIds,
        CancellationToken cancellationToken);
}