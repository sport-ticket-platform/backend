using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class Match
{
    public int MatchId { get; private set; }
    public int LeagueId { get; private set; }
    public int SportId { get; private set; }
    public int VenueId { get; private set; }
    public int HostTeamId { get; private set; }
    public int GuestTeamId { get; private set; }
    public DateTimeOffset MatchTime { get; private set; }

    private Match(int matchId, int leagueId, int sportId, int venueId, int hostTeamId, int guestTeamId,
        DateTimeOffset matchTime)
    {
        MatchId = matchId;
        LeagueId = leagueId;
        SportId = sportId;
        VenueId = venueId;
        HostTeamId = hostTeamId;
        GuestTeamId = guestTeamId;
        MatchTime = matchTime;
    }

    public Match Create(int matchId, int leagueId, int sportId, int venueId, int hostTeamId, int guestTeamId,
        DateTimeOffset matchTime)
    {
        ValidateTheId(matchId, nameof(matchId));
        ValidateTheId(leagueId, nameof(leagueId));
        ValidateTheId(sportId, nameof(sportId));
        ValidateTheId(venueId, nameof(venueId));
        ValidateTheId(hostTeamId, nameof(hostTeamId));
        ValidateTheId(guestTeamId, nameof(guestTeamId));

        int rs = matchTime.CompareTo(DateTimeOffset.Now);
        if (rs < 0)
            throw new DomainException("The match's time supplied is not a valid time");

        return new Match(matchId, leagueId, sportId, venueId, hostTeamId, guestTeamId, matchTime);
    }

    private void ValidateTheId(int id, string name)
    {
        if (int.IsNegative(id))
            throw new DomainException($"The {name} ID must be positive");
    }
}