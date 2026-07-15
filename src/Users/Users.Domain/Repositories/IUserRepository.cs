using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Domain.Repositories;

public interface IUserRepository
{
    public Task UpdateAsync(User user,CancellationToken ct);

    public Task<User?> GetUserByIdAsync(long usedId,CancellationToken ct);

    public Task<int?> GetCityIdByName(string name);
    public Task<UserProfile?> GetUserProfileByIdAsync(long userId, CancellationToken ct);
    
}