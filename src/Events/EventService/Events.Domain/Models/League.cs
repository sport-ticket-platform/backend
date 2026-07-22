using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class League
{
    public int LeagueId { get; private set; }
    public string Name { get; private set; }
    public int SportId { get; private set; }
    
    private League(int leagueId, string name, int sportId)
    {
        LeagueId = leagueId;
        Name = name;
        SportId = sportId;
    }

    public League Create(int leagueId, string name, int sportId)
    {
        if (int.IsNegative(leagueId))
            throw new DomainException("The league ID must be positive");
        
        if (int.IsNegative(sportId))
            throw new DomainException("The sport ID must be positive");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("The league name must be a valid name");

        return new League(leagueId, name, sportId);
    }
    
    
}