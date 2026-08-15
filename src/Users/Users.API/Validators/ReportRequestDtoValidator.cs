using FluentValidation;
using UserService.Users.API.DTOs;
using UserService.Users.Domain.Enums;

namespace UserService.Users.API.Validators;

public class ReportRequestDtoValidator : AbstractValidator<ReportRequestDto>
{
    public ReportRequestDtoValidator()
    {
        RuleFor(r => r.Type)
            .IsEnumName(typeof(ReportType));
        
        RuleFor(r => r.RequestContent)
            .NotEmpty()
            .MaximumLength(500)
            .MinimumLength(10);
    }
    
}