using UserService.Users.Domain.Models;

namespace UserService.Users.Domain.Repositories;

public interface IUserRepository
{
    public Task<User> UpdateAsync(User user);
    public Task<User?> GetUserByIdAsync(long userId, CancellationToken ct);

}