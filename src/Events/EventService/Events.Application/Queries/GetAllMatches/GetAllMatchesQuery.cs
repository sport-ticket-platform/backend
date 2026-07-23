using EventService.Events.Application.Common.Dtos;
using MediatR;

namespace EventService.Events.Application.Queries.GetAllMatches;

public sealed record GetAllMatchesQuery(int Limit,int Offset) : IRequest<IEnumerable<MatchDto>>;
