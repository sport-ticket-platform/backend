using Dapper;
using EventService.Events.Domain.Models;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure.Exceptions;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Queries.GellAllLeagues;

public class GetAllLeaguesQueryHandler : IRequestHandler<GetAllLeaguesQuery,IEnumerable<LeagueDto>>
{
    private ILogger<GetAllLeaguesQueryHandler> _logger;
    private ApplicationDbContext _dbContext;

    public GetAllLeaguesQueryHandler(ILogger<GetAllLeaguesQueryHandler> logger, ApplicationDbContext dbContext)
    {
        _logger = logger;
        _dbContext = dbContext;
    }
    
    public async Task<IEnumerable<LeagueDto>> Handle(GetAllLeaguesQuery request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("fetching all the leagues");
        try
        {
            const string sql = @"
            SELECT
                l.league_id   AS ""LeagueId"",
                l.name        AS ""LeagueName"",
                l.sport_id    AS ""SportId"",
                s.sport_name  AS ""SportName""
            FROM league l
            JOIN sport s ON s.sport_id = l.sport_id
            ORDER BY l.name
            LIMIT @Limit OFFSET @Offset;";

            var command = new CommandDefinition(
                sql,
                new { Limit = request.Limit, Offset = request.Offset },
                cancellationToken: cancellationToken
            );
            return await _dbContext.DbConnection.QueryAsync<LeagueDto>(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the leagues.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching leagues.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching leagues.{state}", ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
        
    }
}