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
[Authorize(policy: "RequireUser")]
public class UserController : ControllerBase
{
    private readonly ILogger<UserController> _logger;
    private readonly IUserService _userService;

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

        if (!long.TryParse(userIdClaim, out var userId))
            return Unauthorized();

        var user = await _userService.GetUserProfileById(userId, ct);
        return Ok(user);
    }

    [HttpPut("profile")]
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
    public async Task<IActionResult> CreateReport([FromBody] ReportReqestDto reportReqestDto, CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var userIdClaim))
            throw new UnauthorizedException("The user must first log in");

        _logger.LogInformation("creating a new report for user {userId}",userIdClaim);
        var reportId = await _userService.CreateReport(userIdClaim, reportReqestDto.RequestConent, reportReqestDto.Type, ct);
        return Ok(reportId);
    }
}