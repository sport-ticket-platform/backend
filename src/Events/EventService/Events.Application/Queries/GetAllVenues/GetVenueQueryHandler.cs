using System.Data;
using System.Data.Common;
using Dapper;
using EventService.Events.Infrastructure.DbContext;
using MediatR;

namespace EventService.Events.Application.Queries.GetAllVenues;

public class GetVenuesQueryHandler : IRequestHandler<GetVenuesQuery, IEnumerable<VenueDto>>
{
    private readonly ILogger<GetVenuesQueryHandler> _logger;
    private readonly ApplicationDbContext _dbContext;

    public GetVenuesQueryHandler(ApplicationDbContext dbContext,ILogger<GetVenuesQueryHandler> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<IEnumerable<VenueDto>> Handle(GetVenuesQuery request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("fetching the list of all venues");
        var connection = (DbConnection)_dbContext.DbConnection;

        if (connection.State != ConnectionState.Open)
            await connection.OpenAsync(cancellationToken);

        const string sql = @"
            SELECT
                v.venue_id     AS ""VenueId"",
                v.name         AS ""VenueName"",
                v.city_id      AS ""CityId"",
                c.name         AS ""CityName"",
                v.street       AS ""Street"",
                v.postal_code  AS ""PostalCode"",
                v.latitude     AS ""Latitude"",
                v.longitude    AS ""Longitude""
            FROM venue v
            JOIN city c ON c.city_id = v.city_id
            ORDER BY v.name
            LIMIT @Limit OFFSET @Offset;";

        return await connection.QueryAsync<VenueDto>(
            new CommandDefinition(sql, new { request.Limit, request.Offset }, cancellationToken: cancellationToken));
    }
}