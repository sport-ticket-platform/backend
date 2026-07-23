using Dapper;
using EventService.Events.Application.Common.Dtos;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure.Exceptions;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Queries.GetTicketConfigsByMatch;

public class
    GetTicketConfigsByMatchQueryHandler : IRequestHandler<GetTicketConfigsByMatchQuery, IEnumerable<TicketConfigDto>>
{
    private ILogger<GetTicketConfigsByMatchQueryHandler> _logger;
    private ApplicationDbContext _dbContext;

    public GetTicketConfigsByMatchQueryHandler(ILogger<GetTicketConfigsByMatchQueryHandler> logger,
        ApplicationDbContext dbContext)
    {
        _logger = logger;
        _dbContext = dbContext;
    }

    public async Task<IEnumerable<TicketConfigDto>> Handle(GetTicketConfigsByMatchQuery request,
        CancellationToken cancellationToken)
    {
        const string sql = @"
            SELECT
                tc.config_id      AS ""ConfigId"",
                tc.match_id       AS ""MatchId"",
                tc.category_id    AS ""CategoryId"",
                cat.name          AS ""CategoryName"",
                tc.price          AS ""Price"",
                tc.total_seats    AS ""TotalSeats"",
                tc.amenities      AS ""Amenities""
            FROM ticket_config tc
            JOIN ticket_category cat ON cat.category_id = tc.category_id
            WHERE tc.match_id = @MatchId
            ORDER BY tc.category_id;";

        var command = new CommandDefinition(
            sql,
            new { MatchId = request.MatchId },
            cancellationToken: cancellationToken
        );
        return await _dbContext.DbConnection.QueryAsync<TicketConfigDto>(command);
    }
}