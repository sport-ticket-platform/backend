using FluentValidation;

namespace EventService.Events.Application.Commands.AddNewMatch;

public class AddNewMatchCommandValidator : AbstractValidator<AddNewMatchCommand>
{
    public AddNewMatchCommandValidator()
    {
        RuleFor(x => x.LeagueId).GreaterThan(0);
        RuleFor(x => x.VenueId).GreaterThan(0);
        RuleFor(x => x.HostTeamId).GreaterThan(0);
        RuleFor(x => x.GuestTeamId).GreaterThan(0);

        RuleFor(x => x.MatchTime)
            .GreaterThan(DateTimeOffset.UtcNow)
            .WithMessage("Match time must be in the future.");

        RuleFor(x => x)
            .Must(x => x.HostTeamId != x.GuestTeamId)
            .WithMessage("Host team and guest team cannot be the same.");
    }
}