using FluentValidation;

namespace EventService.Events.Application.Queries.GetMatches;

public class GetMatchesQueryValidator : AbstractValidator<GetMatchesQuery>
{
    public GetMatchesQueryValidator()
    {
        RuleFor(x => x.SportId).GreaterThan(0).When(x => x.SportId.HasValue);
        RuleFor(x => x.LeagueId).GreaterThan(0).When(x => x.LeagueId.HasValue);
        RuleFor(x => x.CityId).GreaterThan(0).When(x => x.CityId.HasValue);
        RuleFor(x => x.TeamId).GreaterThan(0).When(x => x.TeamId.HasValue);
        RuleFor(x => x.VenueId).GreaterThan(0).When(x => x.VenueId.HasValue);

        RuleFor(x => x.SportName).NotEmpty().When(x => x.SportName is not null);
        RuleFor(x => x.LeagueName).NotEmpty().When(x => x.LeagueName is not null);
        RuleFor(x => x.CityName).NotEmpty().When(x => x.CityName is not null);
        RuleFor(x => x.TeamName).NotEmpty().When(x => x.TeamName is not null);
        RuleFor(x => x.VenueName).NotEmpty().When(x => x.VenueName is not null);

        RuleFor(x => x)
            .Must(x => !x.FromDate.HasValue || !x.ToDate.HasValue || x.FromDate <= x.ToDate)
            .WithMessage("FromDate must be earlier than or equal to ToDate.");
    }
}