using MediatR;

namespace EventService.Events.Application.Commands.AddNewTicketConfig;

public class AddNewTicketConfigCommand : IRequest<int>
{
    public int MatchId { get; set; }
    public int CategoryId { get; set; }
    public decimal Price { get; set; }
    public int TotalSeats { get; set; }
    public string? Amenities { get; set; }
}