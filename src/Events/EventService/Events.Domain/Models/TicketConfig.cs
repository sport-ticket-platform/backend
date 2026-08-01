using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class TicketConfig
{
    public int ConfigId { get; private set; }
    public int MatchId { get; private set; }
    public int CategoryId { get; private set; }
    public decimal Price { get; private set; }
    public int TotalSeats { get; private set; }
    public string? Amenities { get; private set; }

    private TicketConfig(int configId, int matchId, int categoryId, decimal price, int totalSeats,
        string? amenities = null)
    {
        ConfigId = configId;
        MatchId = matchId;
        CategoryId = categoryId;
        Price = price;
        TotalSeats = totalSeats;
        Amenities = amenities;
    }

    public TicketConfig Create(int configId, int matchId, int categoryId, decimal price, int totalSeats,
        string? amenities = null)
    {
        ValidateTheId(configId,nameof(configId));
        ValidateTheId(matchId,nameof(matchId));
        ValidateTheId(categoryId,nameof(categoryId));

        if (decimal.IsNegative(price))
            throw new DomainException("The price cannot be negative");
        
        if(int.IsNegative(totalSeats))
            throw new DomainException("The total seats number must be positive");

        return new TicketConfig(configId, matchId, categoryId, price, totalSeats, amenities);

    }

    public void IncreasePrice(decimal amount)
    {
        if (amount < 0)
            throw new DomainException("The amount to be changed must be positive");
        Price += amount;
    }
    
    public void DecreasePrice(decimal amount)
    {
        if (amount < 0)
            throw new DomainException("The amount to be changed must be positive");
        if (amount > Price)
            throw new DomainException("The amount to be decreased cannot be more than the current price");
       
        Price -= amount;
    }
    
    private void ValidateTheId(int id, string name)
    {
        if (int.IsNegative(id))
            throw new DomainException($"The {name} ID must be positive");
    }
    
}