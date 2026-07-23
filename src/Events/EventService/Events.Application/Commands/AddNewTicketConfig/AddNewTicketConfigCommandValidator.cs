namespace EventService.Events.Application.Commands.AddNewTicketConfig;

using System.Text.Json;
using FluentValidation;

public class AddNewTicketConfigCommandValidator : AbstractValidator<AddNewTicketConfigCommand>
{
    public AddNewTicketConfigCommandValidator()
    {
        RuleFor(x => x.MatchId).GreaterThan(0);
        RuleFor(x => x.CategoryId).GreaterThan(0);
        RuleFor(x => x.Price).GreaterThanOrEqualTo(0);
        RuleFor(x => x.TotalSeats).GreaterThan(0);

        RuleFor(x => x.Amenities)
            .Must(BeValidJson)
            .WithMessage("Amenities must be valid JSON.")
            .When(x => !string.IsNullOrWhiteSpace(x.Amenities));
    }

    private static bool BeValidJson(string? json)
    {
        try
        {
            using var _ = JsonDocument.Parse(json!);
            return true;
        }
        catch (JsonException)
        {
            return false;
        }
    }
}