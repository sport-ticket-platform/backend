using System.Data;
using System.Data.Common;
using Dapper;
using EventService.Events.Application.Commands.AddNewTicketConfig;
using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Domain.Repositories;
using EventService.Events.Infrastructure.DbContext;
using Npgsql;

namespace EventService.Events.Infrastructure.Repositories;

public class WriteRepository : IWriteRepository
{
    private readonly ILogger<WriteRepository> _logger;
    private readonly ApplicationDbContext _dbContext;

    public WriteRepository(ApplicationDbContext dbContext, ILogger<WriteRepository> logger)
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


    public async Task<int> AddTicketConfigAsync(
        int matchId,
        int categoryId,
        decimal price,
        string? amenities,
        int totalSeats,
        List<SeatBlock> seatBlocks,
        CancellationToken cancellationToken)
    {
        _logger.LogInformation(
            "Creating ticket config for MatchId {MatchId}, CategoryId {CategoryId}, TotalSeats {TotalSeats}",
            matchId, categoryId, totalSeats);

        var connection = (DbConnection)_dbContext.DbConnection;

        if (connection.State != ConnectionState.Open)
            await connection.OpenAsync(cancellationToken);

        await using var transaction = await connection.BeginTransactionAsync(cancellationToken);

        try
        {
            const string insertConfigSql = @"
                INSERT INTO ticket_config (match_id, category_id, price, amenities, total_seats)
                VALUES (@MatchId, @CategoryId, @Price, @Amenities::jsonb, @TotalSeats)
                RETURNING config_id;";

            int configId;

            try
            {
                configId = await connection.QuerySingleAsync<int>(
                    new CommandDefinition(insertConfigSql, new
                    {
                        MatchId = matchId,
                        CategoryId = categoryId,
                        Price = price,
                        Amenities = amenities,
                        TotalSeats = totalSeats
                    }, transaction: transaction, cancellationToken: cancellationToken));
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.ForeignKeyViolation)
            {
                _logger.LogWarning(ex,
                    "Foreign key violation creating ticket config for MatchId {MatchId}, CategoryId {CategoryId}",
                    matchId, categoryId);
                throw new NotFoundException("Match or ticket category does not exist.");
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.UniqueViolation)
            {
                _logger.LogWarning(ex,
                    "Duplicate ticket config for MatchId {MatchId}, CategoryId {CategoryId}",
                    matchId, categoryId);
                throw new BusinessLogicException(
                    $"A ticket config for match {matchId} and category {categoryId} already exists.");
            }

            _logger.LogInformation("Ticket config {ConfigId} inserted, checking for seat overlaps", configId);

            var sections = seatBlocks.Select(b => b.Section).ToArray();
            var rowStarts = seatBlocks.Select(b => b.RowStart).ToArray();
            var rowCounts = seatBlocks.Select(b => b.RowCount).ToArray();
            var seatsPerRow = seatBlocks.Select(b => b.SeatsPerRow).ToArray();

            const string overlapCheckSql = @"
                SELECT EXISTS (
                    SELECT 1
                    FROM (
                        SELECT b.section, gs_row.row_no, gs_seat.seat_no
                        FROM unnest(@Sections, @RowStarts, @RowCounts, @SeatsPerRow)
                             AS b(section, row_start, row_count, seats_per_row)
                        CROSS JOIN LATERAL generate_series(b.row_start, b.row_start + b.row_count - 1) AS gs_row(row_no)
                        CROSS JOIN LATERAL generate_series(1, b.seats_per_row) AS gs_seat(seat_no)
                    ) candidate
                    JOIN seat s
                        ON s.section = candidate.section
                       AND s.row_no = candidate.row_no
                       AND s.seat_no = candidate.seat_no
                    JOIN ticket_config tc ON tc.config_id = s.config_id
                    WHERE tc.match_id = @MatchId
                      AND tc.config_id <> @ConfigId
                );";

            var hasOverlap = await connection.QuerySingleAsync<bool>(
                new CommandDefinition(overlapCheckSql, new
                {
                    Sections = sections,
                    RowStarts = rowStarts,
                    RowCounts = rowCounts,
                    SeatsPerRow = seatsPerRow,
                    MatchId = matchId,
                    ConfigId = configId
                }, transaction: transaction, cancellationToken: cancellationToken));

            if (hasOverlap)
            {
                _logger.LogWarning(
                    "Seat overlap detected for MatchId {MatchId}, ConfigId {ConfigId} — rolling back",
                    matchId, configId);
                throw new BusinessLogicException(
                    "One or more seats in the requested layout overlap with an existing ticket config for this match.");
            }

            const string insertSeatsSql = @"
                INSERT INTO seat (config_id, section, row_no, seat_no)
                SELECT @ConfigId, b.section, gs_row.row_no, gs_seat.seat_no
                FROM unnest(@Sections, @RowStarts, @RowCounts, @SeatsPerRow)
                     AS b(section, row_start, row_count, seats_per_row)
                CROSS JOIN LATERAL generate_series(b.row_start, b.row_start + b.row_count - 1) AS gs_row(row_no)
                CROSS JOIN LATERAL generate_series(1, b.seats_per_row) AS gs_seat(seat_no);";

            var seatsInserted = await connection.ExecuteAsync(
                new CommandDefinition(insertSeatsSql, new
                {
                    ConfigId = configId,
                    Sections = sections,
                    RowStarts = rowStarts,
                    RowCounts = rowCounts,
                    SeatsPerRow = seatsPerRow
                }, transaction: transaction, cancellationToken: cancellationToken));

            await transaction.CommitAsync(cancellationToken);

            _logger.LogInformation(
                "Ticket config {ConfigId} committed with {SeatsInserted} seats for MatchId {MatchId}",
                configId, seatsInserted, matchId);

            return configId;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex,
                "Failed to create ticket config for MatchId {MatchId}, CategoryId {CategoryId} — rolling back transaction",
                matchId, categoryId);
            await transaction.RollbackAsync(cancellationToken);
            throw;
        }
    }
}