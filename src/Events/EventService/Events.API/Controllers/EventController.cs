using EventService.Events.Application.Commands.AddNewMatch;
using EventService.Events.Application.Commands.AddNewTicketConfig;
using EventService.Events.Application.Common.Dtos;
using EventService.Events.Application.Queries.GellAllLeagues;
using EventService.Events.Application.Queries.GetAllMatches;
using EventService.Events.Application.Queries.GetAllVenues;
using EventService.Events.Application.Queries.GetMatches;
using EventService.Events.Application.Queries.GetSeatsByConfig;
using EventService.Events.Application.Queries.GetTicketConfigsByMatch;
using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace EventService.Events.API.Controllers;

[ApiController]
[Route("/api/event")]
public class EventController : ControllerBase
{
    private readonly ILogger<EventController> _logger;
    private readonly ISender _sender;
    
    public EventController(ILogger<EventController> logger, ISender sender)
    {
        _logger = logger;
        _sender = sender;
    }

    [Authorize(policy:"RequireUser")]
    [HttpGet("match")]
    public async Task<IActionResult> GetAllMatches([FromQuery] GetAllMatchesQuery query)
    {
        _logger.LogInformation("fetching all matches");

        const int MaxLimit = 40;
        const int DefaultLimit = 20;

        if (query.Limit <= 0)
            query.Limit = DefaultLimit;
        else if (query.Limit > MaxLimit)
            query.Limit = MaxLimit;

        if (query.Offset < 0)
            query.Offset = 0;
        
        var allMatches =  await _sender.Send(query);
        return Ok(allMatches);
    }
    
    [Authorize(policy:"RequireUser")]
    [HttpPost("match")]
    public async Task<IActionResult> GetMatch([FromBody] GetMatchesQuery query)
    {
        _logger.LogInformation("fetching the matches with filtered attributes.");

        const int MaxLimit = 40;
        const int DefaultLimit = 20;

        if (query.Limit <= 0)
            query.Limit = DefaultLimit;
        else if (query.Limit > MaxLimit)
            query.Limit = MaxLimit;

        if (query.Offset < 0)
            query.Offset = 0;
        
        var matches = await _sender.Send(query);
        return Ok(matches);

    }

    [Authorize(policy:"RequireUser")]
    [HttpGet("match/{MatchId}")]
    public async Task<IActionResult> GetTicketConfig([FromRoute] GetTicketConfigsByMatchQuery query)
    {
        _logger.LogInformation("fetching tickets of the match {matchId}",query.MatchId);
       var ticketConfigs =  await _sender.Send(query);
       return Ok(ticketConfigs);
    }

    public async Task<IActionResult> GetSeats(GetSeatsByConfigQuery query)
    {
        _logger.LogInformation("getting all the seats for the config ID {configId}",query.ConfigIds);
        var seats = await _sender.Send(query);
        return Ok(seats);
    }

    [Authorize(policy:"RequireUser")]
    [HttpGet("venues")]
    public async Task<IActionResult> GetAllVenues([FromQuery] GetVenuesQuery query)
    {
        
        _logger.LogInformation("getting all the venues");
        const int MaxLimit = 40;
        const int DefaultLimit = 20;

        if (query.Limit <= 0)
            query.Limit = DefaultLimit;
        else if (query.Limit > MaxLimit)
            query.Limit = MaxLimit;

        if (query.Offset < 0)
            query.Offset = 0;

        var venues = await _sender.Send(query);
        return Ok(venues);
    }
    
    [Authorize(policy:"RequireUser")]
    [HttpGet("leagues")]
    public async Task<IActionResult> GetAllLeagues([FromQuery] GetAllLeaguesQuery query)
    {
        _logger.LogInformation("getting all the leagues");
        const int MaxLimit = 40;
        const int DefaultLimit = 20;

        if (query.Limit <= 0)
            query.Limit = DefaultLimit;
        else if (query.Limit > MaxLimit)
            query.Limit = MaxLimit;

        if (query.Offset < 0)
            query.Offset = 0;

        var venues = await _sender.Send(query);
        return Ok(venues);
    }

    [Authorize(policy:"RequireAdmin")]
    [HttpPost("new/match")]
    public async Task<IActionResult> AddNewMatch(AddNewMatchCommand command,CancellationToken ct)
    {
        _logger.LogInformation("adding new match");
        var matchId = await _sender.Send(command,ct);
        return Ok(matchId);
    }
    
    
    [Authorize(policy:"RequireAdmin")]
    [HttpPost("new/match/{matchId}/ticket")]
    public async Task<IActionResult> AddNewُTicketConfig(AddNewTicketConfigCommand command,CancellationToken ct)
    {
        _logger.LogInformation("adding new ticket");
        var matchId = await _sender.Send(command,ct);
        return Ok(matchId);
    }
}