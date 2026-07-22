using UserService.Users.Application.Requests;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Application.Services;

public interface IUserService
{
    public Task<UserProfile> GetUserProfileById(long userId, CancellationToken ct);
    public Task ChangeAccountStatus(long userId, bool active, CancellationToken ct);
    public Task ChangePassword(long userId, string newPasswordHash, CancellationToken ct);
    public Task UpdateUserProfile(UpdateProfileRequest updateRequest, CancellationToken ct);

    public Task<User> GetUserById(long userId, CancellationToken ct);
    public Task<User> GetUserByEmail(string email, CancellationToken ct);
    public Task<User> GetUserByPhone(string phone, CancellationToken ct);
    public Task<bool> CheckEmailExists(string email, CancellationToken ct);
    public Task<User> CreateUser(User user, CancellationToken ct);

}