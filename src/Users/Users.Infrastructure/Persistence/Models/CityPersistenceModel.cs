namespace UserService.Users.Infrastructure.Persistence.Models;

public class CityPersistenceModel
{
    public int CityId { get; private set; }
    public string Name { get; private set; }

    private CityPersistenceModel(int cityId, string name)
    {
        CityId = cityId;
        Name = name;
    }
}