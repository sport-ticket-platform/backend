using UserService.Users.Domain.ReadModels;

namespace UserService.Users.Domain.Repositories;

public interface IAdminRepository
{
    public Task<List<UserReports>> GetAllOpenReports(CancellationToken ct, int limit = 20, int offset = 0);

}