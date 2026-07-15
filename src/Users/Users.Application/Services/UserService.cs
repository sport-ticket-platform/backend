using UserService.Users.Application.Results;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.Repositories;

namespace UserService.Users.Application.Services;

public class UserService : IUserService
{
    private readonly ILogger<UserService> _logger;
    private readonly IUserRepository _userRepo;

    public UserService(ILogger<UserService> logger, IUserRepository userRepo)
    {
        _logger = logger;
        _userRepo = userRepo;
    }

    public GetUserResult? GetUser(long userId, CancellationToken ct)
    {
        _logger.LogInformation("Getting user's profile with user ID {userId}", userId);
        var user = _userRepo.GetUserByIdAsync(userId, ct);
        
        if (user.Result is null)
            return null;
        
        return new GetUserResult(user.Result.FirstName, user.Result.LastName, user.Result.Email,
            user.Result.PhoneNumber);
    }
}