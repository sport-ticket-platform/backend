using Dapper;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure.DbContext;

namespace UserService.Users.Infrastructure.Repositories;

public class UserRepository : IUserRepository
{
    private ApplicationDbContext _dbContext;
    private ILogger<UserRepository> _logger;
    public UserRepository(ApplicationDbContext dbContext,ILogger<UserRepository> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }
    
    public Task<User> UpdateAsync(User user)
    {
        throw new NotImplementedException();
    }

    public async Task<User?> GetUserByIdAsync(long userId, CancellationToken ct)
    {
        string sql = "SELECT " +
                     "\"first_name\" as \"FirstName\"," +
                     "\"last_name as \"LastName\"" +
                     "\"email\" as Email" +
                     "\"phone_number\" as PhoneNumber " +
                     "FROM \"users\"" +
                     "WHERE user_id = @userId";
        var user = await _dbContext.DbConnection.QueryFirstAsync<User>(sql, new { userId = userId });
        return user;
    }
}