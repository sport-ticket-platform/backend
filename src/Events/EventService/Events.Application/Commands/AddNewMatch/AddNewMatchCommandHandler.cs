using EventService.Events.Application.Common.Exceptions;
using EventService.Events.Domain.Repositories;
using EventService.Events.Infrastructure.DbContext;
using MediatR;

namespace EventService.Events.Application.Commands.AddNewMatch;

using System.Data;
using System.Data.Common;
using Dapper;
using Npgsql;

public class AddNewMatchCommandHandler : IRequestHandler<AddNewMatchCommand, long>
{
    private readonly ILogger<AddNewMatchCommandHandler> _logger;
    private readonly IWriteRepository _writeRepository;

    public AddNewMatchCommandHandler(IWriteRepository writeRepository,ILogger<AddNewMatchCommandHandler> logger)
    {
        _logger = logger;
        _writeRepository = writeRepository;
    }

    public async Task<long> Handle(AddNewMatchCommand request, CancellationToken cancellationToken)
    {
        _logger.LogInformation("adding a new match.");
        return await _writeRepository.AddMatchAsync(
            request.LeagueId,
            request.VenueId,
            request.MatchTime,
            request.HostTeamId,
            request.GuestTeamId,
            cancellationToken);
    }
}