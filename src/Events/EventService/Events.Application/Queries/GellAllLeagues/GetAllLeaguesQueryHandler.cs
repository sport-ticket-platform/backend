using Dapper;
using EventService.Events.Domain.Models;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure.Exceptions;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Queries.GellAllLeagues;

public class GetAllLeaguesQueryHandler : IRequestHandler<GetAllLeaguesQuery, IEnumerable<LeagueDto>>
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
}