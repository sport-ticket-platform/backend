using EventService.Events.Domain.Exceptions;
using EventService.Events.Domain.ValueObjects;

namespace EventService.Events.Domain.Models;

public class Venue
{
    public int VenueId { get; private set; }
    public string Name { get; private set; }
    public Address Address { get; private set; }


    private Venue(int venueId, string name, Address address)
    {
        VenueId = venueId;
        Name = name;
        Address = address;
    }

    public Venue Create(int venueId, string name, Address address)
    {
        if (int.IsNegative(venueId))
            throw new DomainException("The venue ID must be positive");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("The venue's name supplied must be a valid name");

        ArgumentNullException.ThrowIfNull(address);

        return new Venue(venueId, name, address);

    }
}