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
        SELECT *  FROM TABLE (report)
        WHERE status = @Status
        LIMIT @Limit OFFSET @Offset;
        ";

        try
        {
            var command = new CommandDefinition(
                sql, new { Status = ReportStatus.OPEN },
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
        SELECT * FROM table (report)
        WHERE report_id = @ReportId;";

        try
        {
            var command = new CommandDefinition(
                sql,
                new { ReportId = reportId },
                cancellationToken: ct
            );
            return await _dbContext.DbConnection.QuerySingleOrDefaultAsync<Report>(command);
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
            _logger.LogCritical(ex, "Database connection failed while fetching the report {reportId}.",report.ReportId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching the report {reportId}.",report.ReportId);
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
}