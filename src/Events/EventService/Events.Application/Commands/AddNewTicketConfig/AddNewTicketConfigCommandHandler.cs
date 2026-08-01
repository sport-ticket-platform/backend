using System.Data;
using System.Data.Common;
using Dapper;
using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Domain.Repositories;
using EventService.Events.Infrastructure.DbContext;
using MediatR;
using Npgsql;

namespace EventService.Events.Application.Commands.AddNewTicketConfig;

public class AddNewTicketConfigCommandHandler : IRequestHandler<AddNewTicketConfigCommand, int>
{
    private readonly ILogger<AddNewTicketConfigCommandHandler> _logger;
    private readonly IWriteRepository _writeRepository;

    public AddNewTicketConfigCommandHandler(IWriteRepository writeRepository,ILogger<AddNewTicketConfigCommandHandler> logger)
    {
        _writeRepository = writeRepository;
        _logger = logger;
    }
    public async Task<int> Handle(AddNewTicketConfigCommand request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("creating a new ticket config");
        var matchId = await _writeRepository.AddTicketConfigAsync(
            request.MatchId,
            request.CategoryId,
            request.Price,
            request.Amenities,
            request.TotalSeats,
            request.SeatBlocks,
            cancellationToken);
        return matchId;
    }
}