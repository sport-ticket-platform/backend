using UserService.Users.API.DTOs;
using UserService.Users.Application.Exceptions;
using UserService.Users.Domain.Models;
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

    public async Task<Report> GetOpenReport(int reportId, CancellationToken ct)
    {
        _logger.LogInformation("fetching the open report {reportId}",reportId);
        var report = await _adminRepo.GetOpenReport(reportId, ct);
        if (report is null)
            throw new NotFoundException($"The report {reportId} does not exist");
        return report;
    }

    public async Task AnswerReport(int reportId,string response , CancellationToken ct)
    {
        _logger.LogInformation("answering the report {reportId}",reportId);
        var report = await GetOpenReport(reportId, ct);
        
        report.AnswerReport(response,DateTimeOffset.Now);
        
        int row = await _adminRepo.UpdateReport(report, ct);
        if (row == 0)
            throw new NotFoundException($"The report {reportId} does not exist");
    } 
    
    public async Task<List<AdminUserView>> GetUsers(UserFilterDto filter, CancellationToken ct)
    {
        return await _adminRepo.GetUsersByFilter(filter, ct);
    }
    
}