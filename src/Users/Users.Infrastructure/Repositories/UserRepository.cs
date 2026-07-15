using System.Net.Sockets;
using Dapper;
using Npgsql;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure.DbContext;
using UserService.Users.Infrastructure.Exceptions;

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
        try
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
        catch (NpgsqlException ex) when (ex.InnerException is SocketException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user {userId}.",
                userId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching user {UserId}", userId);
            throw new InfrastructureException("Database operation timed out.", ex);

        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex,"Database rejected the query while fetching the user {userId}.",userId);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }
}