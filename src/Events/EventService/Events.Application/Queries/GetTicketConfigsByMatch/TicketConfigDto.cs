namespace EventService.Events.Application.Queries.GetTicketConfigsByMatch;

public class TicketConfigDto
{
    public int ConfigId { get; set; }
    public int MatchId { get; set; }
    public int CategoryId { get; set; }
    public string CategoryName { get; set; }
    public decimal Price { get; set; }
    public int TotalSeats { get; set; }
    public string? Amenities { get; set; }

}