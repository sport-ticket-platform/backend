namespace EventService.Events.Application.Queries.GetSeatsByConfig;

public class SeatDto
{
    public long SeatId { get; set; }
    public int ConfigId { get; set; }
    public int Section { get; set; }
    public int RowNo { get; set; }
    public int SeatNo { get; set; }
    public bool IsReserved { get; set; }
}