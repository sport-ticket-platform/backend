namespace EventService.Events.Application.Commands.AddNewTicketConfig;

public class SeatBlock
{
    public int Section { get; set; }
    public int RowStart { get; set; } = 1;
    public int RowCount { get; set; }
    public int SeatsPerRow { get; set; }
}