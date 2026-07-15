using UserService.Users.Application.Exceptions;
using UserService.Users.Application.Requests;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;
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

    public async Task<UserProfile?> GetUserProfileById(long userId, CancellationToken ct)
    {
        _logger.LogInformation("Fetching user's profile with user ID {userId}", userId);
        var userProfile = await _userRepo.GetUserProfileByIdAsync(userId, ct) ??
                          throw new NotFoundException("The User not found");

        return userProfile;
    }

    public async Task UpdateUserProfile(UpdateProfileRequest updateRequest, CancellationToken ct)
    {
        _logger.LogInformation("Updating user's profile with user ID {userId}", updateRequest.UserId);

        var user = await _userRepo.GetUserByIdAsync(updateRequest.UserId, ct);
        if (user is null)
            throw new NotFoundException("User not found");

        int? cityId = await _userRepo.GetCityIdByName(updateRequest.City) ??
                      throw new NotFoundException("The city not found");
        
        
        user.Update(updateRequest.FirstName, updateRequest.LastName, updateRequest.Email, updateRequest.PhoneNumber,
            cityId ?? 1);
        await _userRepo.UpdateAsync(user, ct);
    }
}