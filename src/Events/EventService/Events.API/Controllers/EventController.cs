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
[Authorize(policy:"RequireUser")]
[Route("/event")]
public class EventController : ControllerBase
{
    private readonly ILogger<EventController> _logger;
    private readonly ISender _sender;
    
    public EventController(ILogger<EventController> logger, ISender sender)
    {
        _logger = logger;
        _sender = sender;
    }

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
}