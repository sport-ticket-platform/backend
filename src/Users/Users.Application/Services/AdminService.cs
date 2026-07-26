using UserService.Users.Domain.ReadModels;
using UserService.Users.Domain.Repositories;

namespace UserService.Users.Application.Services;

public class AdminService : IAdminService
{
    private readonly IAdminRepository _adminRepo;
    private readonly ILogger<AdminService> _logger;

    public AdminService(IAdminRepository adminRepo, ILogger<AdminService> logger)
    {
        _adminRepo = adminRepo;
        _logger = logger;
    }

    
    public async Task<List<UserReports>> GetAllOpenReports(int limit, int offset, CancellationToken ct)
    {
        _logger.LogInformation("fetching all the the reports created by all users");
        var reports = await _adminRepo.GetAllOpenReports(ct,limit, offset);
        return reports;
    }
}