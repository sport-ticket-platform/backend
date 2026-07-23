using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class City
{
    public int SportId { get; private set; }
    public string Name { get; private set; }

    private City(int sportId, string name)
    {
        SportId = sportId;
        Name = name;
    }

    public City Create(int sportId, string name)
    {
        if (int.IsNegative(sportId))
            throw new DomainException("The sport ID must be positive");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("The sport's name supplied is not a valid name");

        return new City(sportId, name);
    }
}