using FluentValidation;
using UserService.Users.API.DTOs;

namespace UserService.Users.API.Validators;

public class ReportResponseDtoValidator : AbstractValidator<ReportResponseDto>
{
    public ReportResponseDtoValidator()
    {
        RuleFor(r => r.Response)
            .NotEmpty()
            .MaximumLength(500).WithMessage("The maximum length of the response message is 500 letters");
    }
}