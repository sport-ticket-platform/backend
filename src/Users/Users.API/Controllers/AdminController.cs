using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using UserService.Users.API.Exceptions;
using UserService.Users.Application.Services;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.API.Controllers;

[ApiController]
[Authorize(policy:"RequireAdmin")]
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
    public async Task<IActionResult> GetAllOpenReports([FromQuery] int limit, [FromQuery] int offset,CancellationToken ct)
    {
        if (!long.TryParse(User.FindFirst("sub")?.Value, out var adminIdClaim))
            throw new UnauthorizedException("The admin must first log in");

        _logger.LogInformation("fetching all reports created by users for admin {adminId}",adminIdClaim);

        if (limit > 100)
            limit = MAXIMUM_LIMIT;
        if (offset < 0)
            offset = DEFAULT_OFFSET;

        var reports = await _adminService.GetAllOpenReports(limit, offset, ct);
        return Ok(reports);
    }
}