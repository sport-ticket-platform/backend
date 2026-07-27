using UserService.Users.API.DTOs;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Domain.Repositories;

public interface IAdminRepository
{
    public Task<List<UserReports>> GetAllOpenReports(CancellationToken ct, int limit = 20, int offset = 0);
    public Task<Report?> GetOpenReport(int reportId, CancellationToken ct);

    public Task<int> UpdateReport(Report report, CancellationToken ct);
    Task<List<AdminUserView>> GetUsersByFilter(UserFilterDto filter, CancellationToken ct);
    public Task UpdateUser(User user, CancellationToken ct);

    public Task<User?> GetUserById(long userId, CancellationToken ct);



}