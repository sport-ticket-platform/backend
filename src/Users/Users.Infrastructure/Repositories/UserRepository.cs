using System.Net.Sockets;
using Dapper;
using Npgsql;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure.DbContext;
using UserService.Users.Infrastructure.Exceptions;

namespace UserService.Users.Infrastructure.Repositories;

public class UserRepository : IUserRepository
{
    private ApplicationDbContext _dbContext;
    private ILogger<UserRepository> _logger;

    public UserRepository(ApplicationDbContext dbContext, ILogger<UserRepository> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task UpdateAsync(User user, CancellationToken ct)
    {
        try
        {
            const string sql = @"
        UPDATE users
        SET
            first_name         = @FirstName,
            last_name           = @LastName,
            role                = @Role::user_role,
            email               = @Email,
            email_verified      = @IsEmailVerified,
            phone_number        = @PhoneNumber,
            phone_verified      = @IsPhoneNumberVerified,
            registration_date   = @RegistrationDate,
            password            = @PasswordHash,
            balance             = @Balance,
            city_id             = @CityId,
            status              = @Status,
            two_factor_enabled  = @IsTwoFactorEnabled
        WHERE user_id = @UserId;
        ";
            await _dbContext.DbConnection.ExecuteAsync(sql, user);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user {userId}.",
                user.UserId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching user {UserId}", user.UserId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user {userId}.{state}", user.UserId,
                ex.SqlState);
            throw new InfrastructureException("DataBase operation failed", ex);
        }
    }

    public Task<bool> DeleteAsync(long userId, CancellationToken ct)
    {
        throw new NotImplementedException();
    }

    public Task<User?> GetUserByIdAsync(long usedId, CancellationToken ct)
    {
        throw new NotImplementedException();
    }

    public Task<int?> GetCityIdByName(string name,CancellationToken ct)
    {
        throw new NotImplementedException();
    }

    public async Task<UserProfile?> GetUserProfileByIdAsync(long userId, CancellationToken ct)
    {
        _logger.LogInformation("fetching user {userId}", userId);

        try
        {
            const string sql = @"
            SELECT
              u.first_name  AS ""FirstName"",
              u.last_name   AS ""LastName"",
              u.email       AS ""Email"",
              u.phone_number AS ""PhoneNumber"",
              u.balance     AS ""Balance"",
              c.name        AS ""City""
              FROM users u
              JOIN city c ON c.city_id = u.city_id
              WHERE u.user_id = @UserId;
              ";
            var userProfile = await _dbContext.DbConnection.QueryFirstAsync<UserProfile>(sql, new { UserId = userId });
            return userProfile;
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
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
            _logger.LogError(ex, "Database rejected the query while fetching the user {userId}.{state}", userId,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }
}