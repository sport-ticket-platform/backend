using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Application.Services;

public interface IAdminService
{
    public Task<List<UserReports>> GetAllOpenReports(int limit, int offset, CancellationToken ct);

}