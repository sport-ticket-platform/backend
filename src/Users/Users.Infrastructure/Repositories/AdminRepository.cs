using System.Text;
using Dapper;
using Npgsql;
using UserService.Users.API.DTOs;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure.DbContext;
using UserService.Users.Infrastructure.Exceptions;
using UserService.Users.Infrastructure.Persistence.Models;

namespace UserService.Users.Infrastructure.Repositories;

public class AdminRepository : IAdminRepository
{
    private readonly ILogger<AdminRepository> _logger;
    private readonly ApplicationDbContext _dbContext;

    public AdminRepository(ILogger<AdminRepository> logger, ApplicationDbContext dbContext)
    {
        _logger = logger;
        _dbContext = dbContext;
    }

    public async Task<List<UserReports>> GetAllOpenReports(CancellationToken ct, int limit = 20, int offset = 0)
    {
        const string sql = @"
        SELECT  
             report_id AS ReportId,
             status AS Status,
             reported_at AS ReportedAt
        FROM TABLE (report)
        WHERE status = @Status
        LIMIT @Limit OFFSET @Offset;
        ";

        try
        {
            var command = new CommandDefinition(
                sql,
                new { Status = ReportStatus.OPEN },
                cancellationToken: ct
            );
            var reports = await _dbContext.DbConnection.QueryAsync<UserReports>(command);
            return reports.ToList();
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching all the reports.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching all the reports.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching all the reports.{state}", ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<Report?> GetOpenReport(int reportId, CancellationToken ct)
    {
        const string sql = @"
        SELECT 
            report_id    AS ""ReportId"",
            user_id      AS ""UserId"",
            type         AS ""Type"",
            reported_at  AS ""ReportedAt"",
            request      AS ""Request"",
            response     AS ""Response"",
            responded_at AS ""RespondedAt"",
            status       AS ""Status""
        FROM table (report)
        WHERE report_id = @ReportId;";

        try
        {
            var command = new CommandDefinition(
                sql,
                new { ReportId = reportId },
                cancellationToken: ct
            );
            var report = await _dbContext.DbConnection.QuerySingleOrDefaultAsync<ReportPersistenceModel>(command);

            if (report is null)
                return null;

            return Report.Create(report.ReportId, report.UserId, report.Type, report.ReportedAt, report.Request,
                report.Response, report.RespondedAt, report.Status);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching all the reports.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching all the reports.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching all the reports.{state}", ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<int> UpdateReport(Report report, CancellationToken ct)
    {
        const string sql = @"
        UPDATE report
        SET
            type = @Type,
            reported_at = @ReportedAt,
            request = @Request,
            response = @Response,
            responded_at = @RespondedAt,
            status = @Status
        WHERE report_id = @ReportId;";
        try
        {
            var command = new CommandDefinition(
                sql,
                report,
                cancellationToken: ct
            );
            return await _dbContext.DbConnection.ExecuteAsync(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the report {reportId}.",
                report.ReportId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching the report {reportId}.", report.ReportId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while updating the report {reportId}.{state}",
                report.ReportId, ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<List<AdminUserView>> GetUsersByFilter(UserFilterDto filter, CancellationToken ct)
    {
        var sqlBuilder = new StringBuilder(@"
        SELECT
            u.user_id    AS ""UserId"",
            u.first_name AS ""FirstName"",
            u.last_name  AS ""LastName"",
            u.email      AS ""Email"",
            u.status     AS ""Status""
        FROM users u
        WHERE 1 = 1
    ");

        var parameters = new DynamicParameters();

        if (!string.IsNullOrWhiteSpace(filter.FirstName))
        {
            sqlBuilder.Append(" AND u.first_name ILIKE @FirstName");
            parameters.Add("FirstName", $"%{filter.FirstName}%");
        }

        if (!string.IsNullOrWhiteSpace(filter.LastName))
        {
            sqlBuilder.Append(" AND u.last_name ILIKE @LastName");
            parameters.Add("LastName", $"%{filter.LastName}%");
        }

        if (!string.IsNullOrWhiteSpace(filter.Email))
        {
            sqlBuilder.Append(" AND u.email ILIKE @Email");
            parameters.Add("Email", $"%{filter.Email}%");
        }

        if (filter.Status.HasValue)
        {
            sqlBuilder.Append(" AND u.status = @Status");
            parameters.Add("Status", filter.Status.Value);
        }

        sqlBuilder.Append(" ORDER BY u.user_id LIMIT @Limit OFFSET @Offset");
        parameters.Add("Limit", filter.Limit);
        parameters.Add("Offset", filter.Offset);

        try
        {
            var command = new CommandDefinition(sqlBuilder.ToString(), parameters, cancellationToken: ct);
            var users = await _dbContext.DbConnection.QueryAsync<AdminUserView>(command);
            return users.ToList();
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching filtered users.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching filtered users.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching filtered users. {state}", ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<User?> GetUserById(long userId, CancellationToken ct)
    {
        try
        {
            const string sql = @"
            SELECT
                user_id            AS ""UserId"",
                first_name         AS ""FirstName"",
                last_name          AS ""LastName"",
                role               AS ""Role"",
                email              AS ""Email"",
                email_verified     AS ""IsEmailVerified"",
                phone_number       AS ""PhoneNumber"",
                phone_verified     AS ""IsPhoneNumberVerified"",
                registration_date  AS ""RegistrationDate"",
                password           AS ""PasswordHash"",
                balance            AS ""Balance"",
                city_id            AS ""CityId"",
                is_active          AS ""IsActive"",
                two_factor_enabled AS ""IsTwoFactorEnabled""
            FROM users
            WHERE user_id = @UserId;
            ";
            var command = new CommandDefinition(
                sql,
                new { UserId = userId },
                cancellationToken: ct
            );
            var user = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<UserPersistenceModel>(command);

            if (user is null)
                return null;

            return User.Create(user.UserId, user.Firstname, user.LastName, user.Role, user.email, user.PhoneNumber,
                user.PasswordHash, user.CityId, user.IsEmailVerified, user.IsPhoneNumberVerified);
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

    public async Task UpdateUser(User user, CancellationToken ct)
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
                is_active           = @IsActive,
                two_factor_enabled  = @IsTwoFactorEnabled
            WHERE user_id = @UserId;
        ";
            var parameters = new
            {
                UserId = user.UserId,
                FirstName = user.FirstName,
                LastName = user.LastName,
                Role = user.Role.ToString(),
                Email = user.Email,
                IsEmailVerified = user.IsEmailVerified,
                PhoneNumber = user.PhoneNumber,
                IsPhoneNumberVerified = user.IsPhoneNumberVerified,
                RegistrationDate = user.RegistrationDate,
                PasswordHash = user.PasswordHash,
                Balance = user.Balance,
                CityId = user.CityId,
                IsActive = user.IsActive,
                IsTwoFactorEnabled = user.IsTwoFactorEnabled
            };

            var command = new CommandDefinition(sql, parameters, cancellationToken: ct);
            await _dbContext.DbConnection.ExecuteAsync(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user {userId}.",
                user.UserId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database operation timed out while fetching user {UserId}", user.UserId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user {userId}.{state}", user.UserId,
                ex.SqlState);
            throw new InfrastructureException("DataBase operation failed", ex);
        }
    }
}