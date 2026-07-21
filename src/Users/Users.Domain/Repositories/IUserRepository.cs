using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Domain.Repositories;

public interface IUserRepository
{
    public Task UpdateAsync(User user,CancellationToken ct);
    public Task<User?> GetUserByIdAsync(long usedId,CancellationToken ct);
    public Task<int?> GetCityIdByName(string name,CancellationToken ct);
    public Task<UserProfile?> GetUserProfileByIdAsync(long userId, CancellationToken ct);

    public Task<User?> GetUserByEmail(string email, CancellationToken ct);

}