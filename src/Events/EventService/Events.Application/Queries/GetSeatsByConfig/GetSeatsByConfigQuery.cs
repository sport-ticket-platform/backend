using MediatR;

namespace EventService.Events.Application.Queries.GetSeatsByConfig;

public class GetSeatsByConfigQuery : IRequest<IEnumerable<SeatDto>>
{
    public IEnumerable<int> ConfigIds { get; set; }
}