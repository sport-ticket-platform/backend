using EventService.Events.Application.Common.Dtos;
using MediatR;

namespace EventService.Events.Application.Queries.GetAllMatches;

public class GetAllMatchesQuery : IRequest<IEnumerable<MatchDto>>
{
    public int Limit { get; set; }
    public int Offset { get; set; }
}
