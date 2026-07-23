using System.Data;
using System.Data.Common;
using Dapper;
using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Infrastructure.DbContext;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Commands.AddNewTicketConfig;

public class AddNewTicketConfigCommandHandler:IRequestHandler<AddNewTicketConfigCommand,int>
{
    private readonly ApplicationDbContext _dbContext;
    private readonly ILogger<AddNewTicketConfigCommandHandler> _logger;

    public AddNewTicketConfigCommandHandler(ApplicationDbContext dbContext,ILogger<AddNewTicketConfigCommandHandler> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<int> Handle(AddNewTicketConfigCommand request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("adding ticket config for the match with match ID {matchId}",request.MatchId);
        
        var connection = (DbConnection)_dbContext.DbConnection;

        if (connection.State != ConnectionState.Open)
            await connection.OpenAsync(cancellationToken);

        const string insertSql = @"
            INSERT INTO ticket_config (match_id, category_id, price, amenities, total_seats)
            VALUES (@MatchId, @CategoryId, @Price, @Amenities::jsonb, @TotalSeats)
            RETURNING config_id;";

        try
        {
            return await connection.QuerySingleAsync<int>(
                new CommandDefinition(
                    insertSql,
                    new
                    {
                        request.MatchId,
                        request.CategoryId,
                        request.Price,
                        request.Amenities,
                        request.TotalSeats
                    },
                    cancellationToken: cancellationToken));
        }
        catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.ForeignKeyViolation)
        {
            var message = ex.ConstraintName switch
            {
                var c when c != null && c.Contains("match_id") =>
                    $"Match with ID {request.MatchId} was not found.",
                var c when c != null && c.Contains("category_id") =>
                    $"Ticket category with ID {request.CategoryId} was not found.",
                _ => "One or more referenced entities do not exist."
            };

            throw new NotFoundException(message);
        }
        catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.UniqueViolation)
        {
            throw new BusinessLogicException(
                $"A ticket config for match {request.MatchId} and category {request.CategoryId} already exists.");
        }
        catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.CheckViolation)
        {
            throw new BusinessLogicException("Price must be non-negative.");
        }
    }
}