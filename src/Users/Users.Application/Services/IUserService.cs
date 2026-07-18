using UserService.Users.Application.Requests;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Application.Services;

public interface IUserService
{
    public Task<UserProfile?> GetUserProfileById(long userId, CancellationToken ct);
    public Task ChangeAccountStatus(long userId, bool active, CancellationToken ct);
    public Task UpdateUserProfile(UpdateProfileRequest updateRequest, CancellationToken ct);

}