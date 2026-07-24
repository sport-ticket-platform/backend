using FluentValidation;

namespace EventService.Events.Application.Queries.GetTicketConfigsByMatch;

public class GetTicketConfigsByMatchValidator : AbstractValidator<GetTicketConfigsByMatchQuery>
{
    public GetTicketConfigsByMatchValidator()
    {
        RuleFor(x => x.MatchId)
            .NotNull()
            .LessThan(0);
    }
}