using EventService.Events.Domain.Models;
using MediatR;

namespace EventService.Events.Application.Queries.GellAllLeagues;

public class GetAllLeaguesQuery : IRequest<IEnumerable<LeagueDto>>
{
    public int Limit { get; set; } = 20;
    public int Offset { get; set; } = 0;
}