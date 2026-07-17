using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Application.Services;

public interface IUserService
{
    public Task<UserProfile?> GetUserProfileById(long userId, CancellationToken ct);
}