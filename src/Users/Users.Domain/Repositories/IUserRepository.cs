using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Domain.Repositories;

public interface IUserRepository
{
    public Task UpdateUser(User user,CancellationToken ct);
    public Task ChangePassword(long userId, string newPasswordHash, CancellationToken ct);
    public Task<User?> GetUserById(long usedId,CancellationToken ct);
    public Task<int?> GetCityIdByName(string name,CancellationToken ct);
    public Task<UserProfile?> GetUserProfileById(long userId, CancellationToken ct);

    public Task<User?> GetUserByEmail(string email, CancellationToken ct);
    
    public Task<User?> GetUserByPhone(string phone, CancellationToken ct);

    public Task<bool> CheckEmailExists(string email, CancellationToken ct);

    public Task<User> CreateUser(User user,CancellationToken ct);

    public Task<bool> CheckPhoneExists(string phone, CancellationToken ct);

    public Task<int> CreateReport(long userId, string requestContent, ReportType type, CancellationToken ct);

    public Task<List<UserReports>> GetAllReports(long userId, CancellationToken ct);
    public Task<Report?> GetReportDetails(long reportId, CancellationToken ct);




}