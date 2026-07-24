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

        RuleFor(x => x.SeatBlocks)
            .NotEmpty().WithMessage("At least one seat block is required.");

        RuleForEach(x => x.SeatBlocks).ChildRules(block =>
        {
            block.RuleFor(b => b.Section).GreaterThan(0);
            block.RuleFor(b => b.RowStart).GreaterThan(0);
            block.RuleFor(b => b.RowCount).GreaterThan(0);
            block.RuleFor(b => b.SeatsPerRow).GreaterThan(0);
        });

        RuleFor(x => x.Amenities)
            .Must(BeValidJson)
            .WithMessage("Amenities must be valid JSON.")
            .When(x => !string.IsNullOrWhiteSpace(x.Amenities));
        
        
        // Catch overlapping row ranges within the SAME section, inside this single request
        RuleFor(x => x.SeatBlocks)
            .Must(HaveNoInternalOverlap)
            .WithMessage("Seat blocks within this request overlap each other.")
            .When(x => x.SeatBlocks.Count > 1);
    }

    private static bool HaveNoInternalOverlap(List<SeatBlock> blocks)
    {
        var bySection = blocks.GroupBy(b => b.Section);
        foreach (var group in bySection)
        {
            var ranges = group
                .Select(b => (Start: b.RowStart, End: b.RowStart + b.RowCount - 1))
                .OrderBy(r => r.Start)
                .ToList();

            for (int i = 1; i < ranges.Count; i++)
                if (ranges[i].Start <= ranges[i - 1].End)
                    return false;
        }

        return true;
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