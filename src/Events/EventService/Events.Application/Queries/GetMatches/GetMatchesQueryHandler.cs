using System.Text;
using Dapper;
using EventService.Events.Application.Common.Dtos;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure.Exceptions;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Queries.GetMatches;

public class GetMatchesQueryHandler : IRequestHandler<GetMatchesQuery, IEnumerable<MatchDto>>
{
    private ILogger<GetMatchesQueryHandler> _logger;
    private ApplicationDbContext _dbContext;

    public GetMatchesQueryHandler(ILogger<GetMatchesQueryHandler> logger, ApplicationDbContext dbContext)
    {
        _logger = logger;
        _dbContext = dbContext;
    }

    public async Task<IEnumerable<MatchDto>> Handle(GetMatchesQuery request, CancellationToken cancellationToken)
    {
        try
        {


            var sqlBuilder = new StringBuilder(@"
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
            WHERE 1 = 1
        ");

            var parameters = new DynamicParameters();

            // --- Sport ---
            if (request.SportId.HasValue)
            {
                sqlBuilder.Append(" AND m.sport_id = @SportId");
                parameters.Add("SportId", request.SportId.Value);
            }
            else if (!string.IsNullOrWhiteSpace(request.SportName))
            {
                sqlBuilder.Append(" AND s.sport_name ILIKE @SportName");
                parameters.Add("SportName", $"%{request.SportName}%");
            }

            // --- League ---
            if (request.LeagueId.HasValue)
            {
                sqlBuilder.Append(" AND m.league_id = @LeagueId");
                parameters.Add("LeagueId", request.LeagueId.Value);
            }
            else if (!string.IsNullOrWhiteSpace(request.LeagueName))
            {
                sqlBuilder.Append(" AND l.name ILIKE @LeagueName");
                parameters.Add("LeagueName", $"%{request.LeagueName}%");
            }

            // --- City ---
            if (request.CityId.HasValue)
            {
                sqlBuilder.Append(" AND v.city_id = @CityId");
                parameters.Add("CityId", request.CityId.Value);
            }
            else if (!string.IsNullOrWhiteSpace(request.CityName))
            {
                sqlBuilder.Append(" AND c.name ILIKE @CityName");
                parameters.Add("CityName", $"%{request.CityName}%");
            }

            // --- Venue ---
            if (request.VenueId.HasValue)
            {
                sqlBuilder.Append(" AND m.venue_id = @VenueId");
                parameters.Add("VenueId", request.VenueId.Value);
            }
            else if (!string.IsNullOrWhiteSpace(request.VenueName))
            {
                sqlBuilder.Append(" AND v.name ILIKE @VenueName");
                parameters.Add("VenueName", $"%{request.VenueName}%");
            }

            // --- Team (host OR guest) ---
            if (request.TeamId.HasValue)
            {
                sqlBuilder.Append(" AND (m.host_team_id = @TeamId OR m.guest_team_id = @TeamId)");
                parameters.Add("TeamId", request.TeamId.Value);
            }
            else if (!string.IsNullOrWhiteSpace(request.TeamName))
            {
                sqlBuilder.Append(" AND (ht.name ILIKE @TeamName OR gt.name ILIKE @TeamName)");
                parameters.Add("TeamName", $"%{request.TeamName}%");
            }

            // --- Date range ---
            if (request.FromDate.HasValue)
            {
                sqlBuilder.Append(" AND m.match_time >= @FromDate");
                parameters.Add("FromDate", request.FromDate.Value);
            }

            if (request.ToDate.HasValue)
            {
                sqlBuilder.Append(" AND m.match_time <= @ToDate");
                parameters.Add("ToDate", request.ToDate.Value);
            }

            sqlBuilder.Append(" ORDER BY m.match_time LIMIT @Limit OFFSET @Offset");
            parameters.Add("Limit", request.Limit);
            parameters.Add("Offset", request.Offset);

            var command = new CommandDefinition(
                sqlBuilder.ToString(),
                parameters,
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