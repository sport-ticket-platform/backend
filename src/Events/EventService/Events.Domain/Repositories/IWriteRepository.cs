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
}