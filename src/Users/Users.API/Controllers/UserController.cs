using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using UserService.Users.API.DTOs;
using UserService.Users.Application.Requests;
using UserService.Users.Application.Services;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class UserController : ControllerBase
{
    private readonly ILogger<UserController> _logger;
    private readonly IUserService _userService;

    public UserController(ILogger<UserController> logger, IUserService userService)
    {
        _logger = logger;
        _userService = userService;
    }


    [HttpGet("/profile")]
    public async Task<ActionResult<UserProfile>> GetUserProfile(CancellationToken ct)
    {
        _logger.LogInformation("fetching user profile");

        var userIdClaim = User.FindFirst("sub")?.Value;

        if (!long.TryParse(userIdClaim, out var userId))
            return Unauthorized();

        var user = await _userService.GetUserProfileById(userId, ct);
        return Ok(user);
    }

    [HttpPut("/profile")]
    public async Task<ActionResult> UpdateUserProfile([FromBody] UserProfileDto userProfileDto, CancellationToken ct)
    {
        _logger.LogInformation("updating user profile");
        
        var updateProfileRequest = new UpdateProfileRequest(
            userProfileDto.UserId,
            userProfileDto.FirstName,
            userProfileDto.LastName,
            userProfileDto.Email,
            userProfileDto.PhoneNumber,
            userProfileDto.City);

        await _userService.UpdateUserProfile(updateProfileRequest, ct);
        return Ok(); 
    }
    
    
    
}