using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class Seat
{
    public int SeatId { get; private set; }
    public int ConfigId { get; private set; }
    public int Section { get; private set; }
    public int RowNo { get; private set; }
    public int SeatNo { get; private set; }

    private Seat(int seatId, int configId, int section, int rowNo, int seatNo)
    {
        SeatId = seatId;
        ConfigId = configId;
        Section = section;
        RowNo = rowNo;
        SeatNo = seatNo;
    }

    public Seat Create(int seatId, int configId, int section, int rowNo, int seatNo)
    {
        Validate(seatId,nameof(seatId));
        Validate(configId,nameof(configId));
        Validate(section,nameof(section));
        Validate(rowNo,nameof(rowNo));
        Validate(seatNo,nameof(seatNo));

        return new Seat(seatId, configId, section, rowNo, seatNo);
    }

    private void Validate(int value, string name)
    {
        if (int.IsNegative(value))
            throw new DomainException($"The {name} cannot be negative");
    }
    
}