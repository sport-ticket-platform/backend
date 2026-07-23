namespace EventService.Events.Application.Interfaces;

public interface IReservationServiceClient
{
    Task<IEnumerable<long>> GetUnreservedSeatIdsAsync(
        IEnumerable<int> configIds,
        CancellationToken cancellationToken);
}