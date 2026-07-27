using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using UserService.Users.API.DTOs;
using UserService.Users.API.Exceptions;
using UserService.Users.Application.Services;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.API.Controllers;

[ApiController]
[AllowAnonymous]
[Route("/api/[controller]")]
public class AdminController : ControllerBase
{
    private readonly ILogger<AdminController> _logger;
    private readonly IAdminService _adminService;
    private const int MAXIMUM_LIMIT = 40;
    private const int DEFAULT_OFFSET = 0;

    public AdminController(ILogger<AdminController> logger, IAdminService adminService)
    {
        _logger = logger;
        _adminService = adminService;
    }

    [HttpGet("report")]
    public async Task<IActionResult> GetAllOpenReports([FromQuery] int limit, [FromQuery] int offset,
        CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var adminIdClaim))
            throw new UnauthorizedException("The admin must first log in");

        _logger.LogInformation("fetching all reports created by users for admin {adminId}", adminIdClaim);

        if (limit > 40)
            limit = MAXIMUM_LIMIT;
        if (offset < 0)
            offset = DEFAULT_OFFSET;

        var reports = await _adminService.GetAllOpenReports(limit, offset, ct);
        return Ok(reports);
    }

    [HttpGet("report/{reportId}")]
    public async Task<IActionResult> GetOpenReport([FromQuery] int reportId, CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var adminIdClaim))
            throw new UnauthorizedException("The admin must first log in");

        _logger.LogInformation("fetching report {reportId} for admin {adminId}", reportId, adminIdClaim);
        var report = await _adminService.GetOpenReport(reportId, ct);
        return Ok(report);
    }

    [HttpPut("report/{reportId}")]
    public async Task<IActionResult> AnswerReport([FromQuery] int reportId, [FromBody] ReportResponseDto responseDto,
        CancellationToken ct)
    {
        _logger.LogInformation("answering the report {reportId}", reportId);
        await _adminService.AnswerReport(reportId, responseDto.Response, ct);
        return Ok();
    }
    
    [HttpGet("users")]
    public async Task<IActionResult> GetUsers([FromQuery] UserFilterDto filter, CancellationToken ct)
    {
        _logger.LogInformation("fetching users");
        var users = await _adminService.GetUsers(filter, ct);
        return Ok(users);
    }

    [HttpPut("users/{userId}")]
    public async Task<IActionResult> ChangeAccountStatus([FromRoute] int userId, [FromQuery] bool active ,CancellationToken ct)
    {
        _logger.LogInformation("Changing user account status");
        await _adminService.ChangeAccountStatus(userId, active, ct);
        return Ok();
    }
}