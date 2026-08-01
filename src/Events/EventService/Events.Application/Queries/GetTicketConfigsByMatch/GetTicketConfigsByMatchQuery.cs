using MediatR;

namespace EventService.Events.Application.Queries.GetTicketConfigsByMatch;

public class GetTicketConfigsByMatchQuery : IRequest<IEnumerable<TicketConfigDto>>
{
    public int MatchId { get; set; }
}