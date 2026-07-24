using System.Data;
using System.Data.Common;
using Dapper;
using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Infrastructure.DbContext;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Commands.AddNewTicketConfig;

public class AddNewTicketConfigCommandHandler : IRequestHandler<AddNewTicketConfigCommand, int>
{
    private readonly ApplicationDbContext _dbContext;

    public AddNewTicketConfigCommandHandler(ApplicationDbContext dbContext)
    {
        _dbContext = dbContext;
    }
    public async Task<int> Handle(AddNewTicketConfigCommand request, CancellationToken cancellationToken)
    {
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
                        request.MatchId,
                        request.CategoryId,
                        request.Price,
                        request.Amenities,
                        request.TotalSeats
                    }, transaction: transaction, cancellationToken: cancellationToken));
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.ForeignKeyViolation)
            {
                throw new NotFoundException("Match or ticket category does not exist.");
            }
            catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.UniqueViolation)
            {
                throw new BusinessLogicException(
                    $"A ticket config for match {request.MatchId} and category {request.CategoryId} already exists.");
            }

            var sections = request.SeatBlocks.Select(b => b.Section).ToArray();
            var rowStarts = request.SeatBlocks.Select(b => b.RowStart).ToArray();
            var rowCounts = request.SeatBlocks.Select(b => b.RowCount).ToArray();
            var seatsPerRow = request.SeatBlocks.Select(b => b.SeatsPerRow).ToArray();

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
                    request.MatchId,
                    ConfigId = configId
                }, transaction: transaction, cancellationToken: cancellationToken));

            if (hasOverlap)
                throw new BusinessLogicException(
                    "One or more seats in the requested layout overlap with an existing ticket config for this match.");

            const string insertSeatsSql = @"
                INSERT INTO seat (config_id, section, row_no, seat_no)
                SELECT @ConfigId, b.section, gs_row.row_no, gs_seat.seat_no
                FROM unnest(@Sections, @RowStarts, @RowCounts, @SeatsPerRow)
                     AS b(section, row_start, row_count, seats_per_row)
                CROSS JOIN LATERAL generate_series(b.row_start, b.row_start + b.row_count - 1) AS gs_row(row_no)
                CROSS JOIN LATERAL generate_series(1, b.seats_per_row) AS gs_seat(seat_no);";

            await connection.ExecuteAsync(
                new CommandDefinition(insertSeatsSql, new
                {
                    ConfigId = configId,
                    Sections = sections,
                    RowStarts = rowStarts,
                    RowCounts = rowCounts,
                    SeatsPerRow = seatsPerRow
                }, transaction: transaction, cancellationToken: cancellationToken));

            await transaction.CommitAsync(cancellationToken);
            return configId;
        }
        catch
        {
            await transaction.RollbackAsync(cancellationToken);
            throw;
        }
    }
}