using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Application.Services;

public interface IAdminService
{
    public Task<List<UserReports>> GetAllOpenReports(int limit, int offset, CancellationToken ct);
    public Task<Report> GetOpenReport(int reportId, CancellationToken ct);

    public Task AnswerReport(int reportId, string response, CancellationToken ct);



}