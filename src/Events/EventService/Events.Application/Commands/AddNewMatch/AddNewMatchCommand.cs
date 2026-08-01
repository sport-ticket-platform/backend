using MediatR;

namespace EventService.Events.Application.Commands.AddNewMatch;

public class AddNewMatchCommand : IRequest<long>
{
    public int LeagueId { get; set; }
    public int VenueId { get; set; }
    public DateTimeOffset MatchTime { get; set; }
    public int HostTeamId { get; set; }
    public int GuestTeamId { get; set; }
}