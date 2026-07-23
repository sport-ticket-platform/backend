using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Infrastructure.DbContext;
using MediatR;

namespace EventService.Events.Application.Commands.AddNewMatch;

using System.Data;
using System.Data.Common;
using Dapper;
using Npgsql;

public class AddNewMatchCommandHandler : IRequestHandler<AddNewMatchCommand, long>
{
    private readonly ApplicationDbContext _dbContext;

    public AddNewMatchCommandHandler(ApplicationDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<long> Handle(AddNewMatchCommand request, CancellationToken cancellationToken)
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
                    new { request.LeagueId },
                    transaction: transaction,
                    cancellationToken: cancellationToken));

            if (sportId is null)
                throw new NotFoundException($"League with ID {request.LeagueId} was not found.");

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
                            request.LeagueId,
                            SportId = sportId.Value,
                            request.VenueId,
                            request.MatchTime,
                            request.HostTeamId,
                            request.GuestTeamId
                        },
                        transaction: transaction,
                        cancellationToken: cancellationToken));
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.ForeignKeyViolation)
            {
                throw new NotFoundException(
                    "One or more referenced entities (venue, host team, or guest team) do not exist.");
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.CheckViolation)
            {
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