using System.Data;
using System.Data.Common;
using Dapper;
using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Domain.Repositories;
using EventService.Events.Infrastructure.DbContext;
using Npgsql;

namespace EventService.Events.Infrastructure.Repositories;

public class WriteRepository : IWriteRepository
{
    private readonly ILogger<WriteRepository> _logger;
    private readonly ApplicationDbContext _dbContext;

    public WriteRepository(ApplicationDbContext dbContext,ILogger<WriteRepository> logger)
    {
        _logger = logger;
        _dbContext = dbContext;
    }

    public async Task<long> AddMatchAsync(
        int leagueId,
        int venueId,
        DateTimeOffset matchTime,
        int hostTeamId,
        int guestTeamId,
        CancellationToken cancellationToken)
    {
        var connection = (DbConnection)_dbContext.DbConnection;

        if (connection.State != ConnectionState.Open)
            await connection.OpenAsync(cancellationToken);

        await using var transaction = await connection.BeginTransactionAsync(cancellationToken);

        try
        {
            const string getLeagueSportSql = @"
                SELECT sport_id
                FROM league
                WHERE league_id = @LeagueId;";

            var sportId = await connection.QuerySingleOrDefaultAsync<int?>(
                new CommandDefinition(
                    getLeagueSportSql,
                    new { LeagueId = leagueId },
                    transaction: transaction,
                    cancellationToken: cancellationToken));

            if (sportId is null)
                throw new NotFoundException($"League with ID {leagueId} was not found.");

            const string insertSql = @"
                INSERT INTO ""match"" (league_id, sport_id, venue_id, match_time, host_team_id, guest_team_id)
                VALUES (@LeagueId, @SportId, @VenueId, @MatchTime, @HostTeamId, @GuestTeamId)
                RETURNING match_id;";

            long matchId;

            try
            {
                matchId = await connection.QuerySingleAsync<long>(
                    new CommandDefinition(
                        insertSql,
                        new
                        {
                            LeagueId = leagueId,
                            SportId = sportId.Value,
                            VenueId = venueId,
                            MatchTime = matchTime,
                            HostTeamId = hostTeamId,
                            GuestTeamId = guestTeamId
                        },
                        transaction: transaction,
                        cancellationToken: cancellationToken));
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.ForeignKeyViolation)
            {
                _logger.LogError("One or more referenced entities (venue, host team, or guest team) do not exist.");
                throw new NotFoundException(
                    "One or more referenced entities (venue, host team, or guest team) do not exist.");
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.CheckViolation)
            {
                _logger.LogError("Host team and guest team cannot be the same.");
                throw new BusinessLogicException("Host team and guest team cannot be the same.");
            }

            await transaction.CommitAsync(cancellationToken);
            return matchId;
        }
        catch
        {
            await transaction.RollbackAsync(cancellationToken);
            throw;
        }
    }
}