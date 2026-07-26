using UserService.Users.Domain.Exceptions;

namespace UserService.Users.Domain.Models;

public class City
{
    public int CityId { get; private set; }
    public string Name { get; private set; }

    private City(int cityId, string name)
    {
        CityId = cityId;
        Name = name;
    }

    public City Create(int cityId, string name)
    {
        if (int.IsNegative(cityId))
            throw new DomainException("The city ID must be positive");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("The city's name supplied is not a valid name");

        return new City(cityId, name);
    }
}