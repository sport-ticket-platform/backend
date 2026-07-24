using MediatR;

namespace EventService.Events.Application.Queries.GetAllVenues;

public class GetVenuesQuery : IRequest<IEnumerable<VenueDto>>
{
    public int Limit { get; set; } = 20;
    public int Offset { get; set; } = 0;
}