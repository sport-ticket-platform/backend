using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.ValueObjects;

public record Address
{
    public string Street { get; }
    public int CityId { get; }
    public string PostalCode { get; }
    public decimal? Latitude { get; }
    public decimal? Longitude { get; }

    private Address(string street, int cityId,
        string postalCode, decimal? latitude, decimal? longitude)
    {
        Street = street;
        CityId = cityId;
        PostalCode = postalCode;
        Latitude = latitude;
        Longitude = longitude;
    }

    public static Address Create(string street, int cityId, string postalCode,
        decimal? latitude = null, decimal? longitude = null)
    {
        if (string.IsNullOrWhiteSpace(street))
            throw new DomainException("Street is required");

        if (string.IsNullOrWhiteSpace(postalCode))
            throw new DomainException("Postal code is required");

        return new Address(street, cityId, postalCode, latitude, longitude);
    }
}