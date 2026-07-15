using UserService.Users.Application.Results;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Application.Services;

public interface IUserService
{
    public UserProfile? GetUserById(long userId, CancellationToken ct);
}