using FluentValidation;

namespace EventService.Events.Application.Queries.GetSeatsByConfig;

public class GetSeatsByConfigQueryValidator : AbstractValidator<GetSeatsByConfigQuery>
{
    public GetSeatsByConfigQueryValidator()
    {
        RuleFor(x => x.ConfigIds)
            .NotNull().WithMessage("ConfigIds must be provided.");

        RuleFor(x => x.ConfigIds)
            .Must(ids => ids.Any()).WithMessage("ConfigIds cannot be empty.")
            .When(x => x.ConfigIds is not null);
    }
}