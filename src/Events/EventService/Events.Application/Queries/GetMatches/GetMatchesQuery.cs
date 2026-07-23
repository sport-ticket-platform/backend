using EventService.Events.Application.Common.Dtos;
using MediatR;

namespace EventService.Events.Application.Queries.GetMatches;

public class GetMatchesQuery : IRequest<IEnumerable<MatchDto>>
{
    public int? SportId { get; set; }
    public string? SportName { get; set; }

    public int? LeagueId { get; set; }
    public string? LeagueName { get; set; }

    public int? CityId { get; set; }
    public string? CityName { get; set; }

    public int? TeamId { get; set; }       
    public string? TeamName { get; set; }  

    public int? VenueId { get; set; }
    public string? VenueName { get; set; }

    public DateTimeOffset? FromDate { get; set; }
    public DateTimeOffset? ToDate { get; set; }

    public int Limit { get; set; } = 20;
    public int Offset { get; set; } = 0;
}