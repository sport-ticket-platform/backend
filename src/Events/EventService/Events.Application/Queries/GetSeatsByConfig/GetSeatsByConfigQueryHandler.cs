using Dapper;
using EventService.Events.Application.Interfaces;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure.Exceptions;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Queries.GetSeatsByConfig;

public class GetSeatsByConfigQueryHandler : IRequestHandler<GetSeatsByConfigQuery, IEnumerable<SeatDto>>
{
    private ILogger<GetSeatsByConfigQueryHandler> _logger;
    private readonly ApplicationDbContext _dbContext;
    private readonly IReservationServiceClient _reservationServiceClient;

    public GetSeatsByConfigQueryHandler(
        ILogger<GetSeatsByConfigQueryHandler> logger,
        ApplicationDbContext dbContext,
        IReservationServiceClient reservationServiceClient)
    {
        _logger = logger;
        _dbContext = dbContext;
        _reservationServiceClient = reservationServiceClient;
    }

    public async Task<IEnumerable<SeatDto>> Handle(GetSeatsByConfigQuery request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("Fetching all the seats with their reservation status for specified config Ids {configIds}",
            request.ConfigIds);
        try
        {
            const string sql = @"
            SELECT
                s.seat_id    AS ""SeatId"",
                s.config_id  AS ""ConfigId"",
                s.section    AS ""Section"",
                s.row_no     AS ""RowNo"",
                s.seat_no    AS ""SeatNo""
            FROM seat s
            WHERE s.config_id = ANY(@ConfigIds)
            ORDER BY s.config_id, s.section, s.row_no, s.seat_no;";

            
            var command = new CommandDefinition(
                sql,
                new { request.ConfigIds },
                cancellationToken: cancellationToken
            );
            var seats = (await _dbContext.DbConnection.QueryAsync<SeatDto>(command)).ToList();

            if (seats.Count == 0)
                return seats;

            var unreservedSeatIds = new HashSet<long>(
                await _reservationServiceClient.GetUnreservedSeatIdsAsync(request.ConfigIds, cancellationToken));

            foreach (var seat in seats)
                seat.IsReserved = !unreservedSeatIds.Contains(seat.SeatId);

            return seats;
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the seats for configIds {configIds}.",
                request.ConfigIds);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching the seats for configIds {configIds}.",
                request.ConfigIds);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex,
                "Database rejected the query while fetching the seats for configIds {configIds}.{state}",
                request.ConfigIds, ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }
}