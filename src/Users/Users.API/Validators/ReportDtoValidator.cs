using FluentValidation;
using UserService.Users.API.DTOs;

namespace UserService.Users.API.Validators;

public class ReportDtoValidator : AbstractValidator<ReportDto>
{
    public ReportDtoValidator()
    {
        RuleFor(r => r.Type)
            .IsInEnum();
        
        RuleFor(r => r.RequestConent)
            .NotEmpty()
            .MaximumLength(500)
            .MinimumLength(10);
    }
    
}