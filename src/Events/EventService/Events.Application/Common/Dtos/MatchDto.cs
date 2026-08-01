namespace EventService.Events.Application.Common.Dtos;

public record MatchDto
{
    public long MatchId { get; set; }
    public DateTimeOffset MatchTime { get; set; }
    public int LeagueId { get; set; }
    public string LeagueName { get; set; }
    public int SportId { get; set; }
    public string SportName { get; set; }
    public int VenueId { get; set; }
    public string VenueName { get; set; }
    public int VenueCityId { get; set; }
    public string VenueCityName { get; set; }
    public int HostTeamId { get; set; }
    public string HostTeamName { get; set; }
    public int GuestTeamId { get; set; }
    public string GuestTeamName { get; set; }
}