using FluentValidation;
using UserService.Users.API.DTOs;

namespace UserService.Users.API.Validators;

public class UserFilterDtoValidator : AbstractValidator<UserFilterDto>
{
    public UserFilterDtoValidator()
    {
        RuleFor(x => x.FirstName).NotEmpty().When(x => x.FirstName is not null);
        RuleFor(x => x.LastName).NotEmpty().When(x => x.LastName is not null);

        RuleFor(x => x.Email)
            .EmailAddress().WithMessage("Email is not a valid email address.")
            .When(x => x.Email is not null);

        RuleFor(x => x.Limit)
            .GreaterThan(0).WithMessage("Limit must be greater than 0.")
            .LessThanOrEqualTo(100).WithMessage("Limit cannot exceed 100.");

        RuleFor(x => x.Offset)
            .GreaterThanOrEqualTo(0).WithMessage("Offset cannot be negative.");
    }
}