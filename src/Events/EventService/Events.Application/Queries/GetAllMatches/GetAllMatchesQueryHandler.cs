using Dapper;
using EventService.Events.Application.Common.Dtos;
using EventService.Events.Domain.Repositories;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure.Exceptions;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Queries.GetAllMatches;

public class GetAllMatchesQueryHandler : IRequestHandler<GetAllMatchesQuery, IEnumerable<MatchDto>>
{
    private ILogger<GetAllMatchesQueryHandler> _logger;
    private ApplicationDbContext _dbContext;

    public GetAllMatchesQueryHandler(ILogger<GetAllMatchesQueryHandler> logger, ApplicationDbContext dbContext)
    {
        _logger = logger;
        _dbContext = dbContext;
    }

    public async Task<IEnumerable<MatchDto>> Handle(GetAllMatchesQuery request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("fetching all matches");

        try
        {
            const string sql = @"
            SELECT
                m.match_id       AS ""MatchId"",
                m.match_time     AS ""MatchTime"",
                m.league_id      AS ""LeagueId"",
                l.name           AS ""LeagueName"",
                m.sport_id       AS ""SportId"",
                s.sport_name     AS ""SportName"",
                m.venue_id       AS ""VenueId"",
                v.name           AS ""VenueName"",
                v.city_id        AS ""VenueCityId"",
                c.name           AS ""VenueCityName"",
                m.host_team_id   AS ""HostTeamId"",
                ht.name          AS ""HostTeamName"",
                m.guest_team_id  AS ""GuestTeamId"",
                gt.name          AS ""GuestTeamName""
            FROM ""match"" m
            JOIN league l   ON l.league_id = m.league_id
            JOIN sport s    ON s.sport_id = m.sport_id
            JOIN venue v    ON v.venue_id = m.venue_id
            JOIN city c     ON c.city_id = v.city_id
            JOIN team ht    ON ht.team_id = m.host_team_id
            JOIN team gt    ON gt.team_id = m.guest_team_id
            ORDER BY m.match_time
            LIMIT @Limit OFFSET @Offset;";

            var command = new CommandDefinition(
                sql,
                new { Limit = request.Limit, Offset = request.Offset },
                cancellationToken: cancellationToken
            );
            return await _dbContext.DbConnection.QueryAsync<MatchDto>(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the matches.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching matches.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching matches.{state}", ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }
}