using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class Team
{
    public int TeamId { get; private set; }
    public string TeamName { get; private set; }
    public int SportId { get; private set; }
    public int CityId { get; private set; }

    private Team(int teamId, string teamName, int sportId, int cityId)
    {
        TeamId = teamId;
        TeamName = teamName;
        SportId = sportId;
        CityId = cityId;
    }

    public Team Create(int teamId, string teamName, int sportId, int cityId)
    {
        if (int.IsNegative(teamId))
            throw new DomainException("The team ID must be positive");
        
        if (int.IsNegative(sportId))
            throw new DomainException("The sport ID must be positive");

        if (int.IsNegative(cityId))
            throw new DomainException("The city ID must be positive");

        if (string.IsNullOrWhiteSpace(teamName))
            throw new DomainException("The team name is not a valid name");

        return new Team(teamId, teamName, sportId, cityId);

    }
    
}