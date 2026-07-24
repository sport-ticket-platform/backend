using EventService.Events.Application.Common.Dtos;
using EventService.Events.Application.Queries.GetAllMatches;
using EventService.Events.Application.Queries.GetMatches;
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
        const int MaxLimit = 40;
        const int DefaultLimit = 20;

        if (query.Limit <= 0)
            query.Limit = DefaultLimit;
        else if (query.Limit > MaxLimit)
            query.Limit = MaxLimit;

        if (query.Offset < 0)
            query.Offset = 0;
        
        _logger.LogInformation("fetching all matches");
        var allMatches =  await _sender.Send(query);
        return Ok(allMatches);
    }
    
    [HttpPost("match")]
    public async Task<IActionResult> GetMatch([FromBody] GetMatchesQuery query)
    {
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

    [HttpGet("match/{ConfigId}")]
    public async Task<IActionResult> GetTicketConfig([FromRoute] GetTicketConfigsByMatchQuery query)
    {
       var ticketConfigs =  await _sender.Send(query);
       return Ok(ticketConfigs);
    }
}