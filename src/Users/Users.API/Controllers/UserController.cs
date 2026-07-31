using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using UserService.Users.API.DTOs;
using UserService.Users.API.Exceptions;
using UserService.Users.Application.Requests;
using UserService.Users.Application.Services;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.API.Controllers;

[ApiController]
[Route("/api/user")]
[Authorize(policy:"RequireUser")]
public class UserController : ControllerBase
{
    private readonly ILogger<UserController> _logger;
    private readonly IUserService _userService;
    private const int MAXIMUM_LIMIT = 40;
    private const int DEFAULT_OFFSET = 0;


    public UserController(ILogger<UserController> logger, IUserService userService)
    {
        _logger = logger;
        _userService = userService;
    }


    [HttpGet("profile")]
    public async Task<ActionResult<UserProfile>> GetUserProfile(CancellationToken ct)
    {
        _logger.LogInformation("fetching user profile");

        var userIdClaim = User.FindFirst("sub")?.Value;
        _logger.LogInformation("user with ID {userId}",userIdClaim);
        _logger.LogInformation("user with ID {claims}",User.Identity.IsAuthenticated);

        
        if (!long.TryParse(userIdClaim, out var userId))
            return Unauthorized();

        var user = await _userService.GetUserProfileById(userId, ct);
        return Ok(user);
    }

    [HttpPut("profile")]
    public async Task<ActionResult> UpdateUserProfile([FromBody] UserProfileDto userProfileDto, CancellationToken ct)
    {
        _logger.LogInformation("updating user profile");

        if (!long.TryParse(User.FindFirst("sub")?.Value, out var userIdClaim))
            throw new UnauthorizedException("The user must first log in");
        
        var updateProfileRequest = new UpdateProfileRequest(
            userIdClaim,
            userProfileDto.FirstName,
            userProfileDto.LastName,
            userProfileDto.Email,
            userProfileDto.PhoneNumber,
            userProfileDto.City);

        await _userService.UpdateUserProfile(updateProfileRequest, ct);
        return Ok();
    }

    [HttpGet("report/{reportId}")]
    public async Task<IActionResult> GetReportDetails(long reportId, CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var userIdClaim))
            throw new UnauthorizedException("The user must first log in");

        _logger.LogInformation("fetching report {reportId} for user {userId}", reportId, userIdClaim);

        var report = await _userService.GetReportDetails(reportId, ct);
        if (report.UserId != userIdClaim)
            throw new ArgumentException("The ID that is passed doesnt match the user credentials");

        return Ok(report);
    }

    [HttpGet("report")]
    public async Task<IActionResult> GetAllReports(CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var userIdClaim))
            throw new UnauthorizedException("The user must first log in");

        _logger.LogInformation("fetching all reports for user {userId}", userIdClaim);

        var reports = await _userService.GetAllReports(userIdClaim, ct);
        return Ok(reports);
    }

    [HttpPost("report")]
    public async Task<IActionResult> CreateReport([FromBody] ReportRequestDto reportRequestDto, CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var userIdClaim))
            throw new UnauthorizedException("The user must first log in");

        _logger.LogInformation("creating a new report for user {userId}", userIdClaim);
        var reportId =
            await _userService.CreateReport(userIdClaim, reportRequestDto.RequestConent, reportRequestDto.Type, ct);
        return Ok(reportId);
    }

    // [HttpGet("cities")]
    // public async Task<IActionResult> GetAllCities(CancellationToken ct)
    // {
    //     _logger.LogInformation("fetching all the users");
    //     var cities = await _userService.GetAllCities(ct);
    //     return Ok(cities);
    // }

    [HttpGet("cities")]
    public async Task<IActionResult> SearchCity([FromQuery] string? searchTerm, CancellationToken ct,
        [FromQuery] int limit = 20, [FromQuery] int offset = 0)
    {
        if (limit > 40)
            limit = MAXIMUM_LIMIT;
        if (offset < 0)
            offset = DEFAULT_OFFSET;

        _logger.LogInformation("fetching city like {searchTerm}", searchTerm);
        var cities = await _userService.SearchCities(searchTerm, limit, offset, ct);
        return Ok(cities);
    }
}