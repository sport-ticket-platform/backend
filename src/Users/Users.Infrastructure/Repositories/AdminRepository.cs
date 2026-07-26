using Dapper;
using Npgsql;
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
        LIMIT @Limit OFFSET @Offset
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
        WHERE report_id = @ReportId";

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
}