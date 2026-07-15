using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Domain.Repositories;

public interface IUserRepository
{
    public Task<User> UpdateAsync(User user);
    public Task<UserProfile?> GetUserByIdAsync(long userId, CancellationToken ct);

}