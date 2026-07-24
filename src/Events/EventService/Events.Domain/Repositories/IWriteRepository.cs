using EventService.Events.Application.Commands.AddNewTicketConfig;

namespace EventService.Events.Domain.Repositories;

public interface IWriteRepository
{
    Task<long> AddMatchAsync(
        int leagueId,
        int venueId,
        DateTimeOffset matchTime,
        int hostTeamId,
        int guestTeamId,
        CancellationToken cancellationToken);
    
    
    Task<int> AddTicketConfigAsync(
        int matchId,
        int categoryId,
        decimal price,
        string? amenities,
        int totalSeats,
        List<SeatBlock> seatBlocks,
        CancellationToken cancellationToken);

}