using FluentValidation;
using UserService.Users.API.DTOs;

namespace UserService.Users.API.Validators;

public class UserProfileDtoValidator : AbstractValidator<UserProfileDto>
{
    public UserProfileDtoValidator()
    {
        RuleFor(userProfile => userProfile.FirstName)
            .NotEmpty()
            .NotEqual(userProfile => userProfile.LastName)
            .MinimumLength(3)
            .MaximumLength(60);

        RuleFor(userProfile => userProfile.LastName)
            .NotEmpty()
            .NotEqual(userProfile => userProfile.FirstName)
            .MinimumLength(3)
            .MaximumLength(60);

        RuleFor(userProfile => userProfile.Email).EmailAddress();

        When(userProfile => userProfile.PhoneNumber is not null, () =>
        {
            RuleFor(userProfile => userProfile.PhoneNumber)
                .Length(11).WithMessage("Phone number must be 11 characters long.")
                .Must(ValidatePhoneNumber!)
                .WithMessage("The phone number must start with \"09\" and all should be digits");
        });

        When(userProfile => userProfile.City is not null, () =>
        {
            RuleFor(userProfile => userProfile.City)
                .NotEmpty().WithMessage("City cannot be empty.");
        });
    }

    private bool ValidatePhoneNumber(string phoneNumber)
    {
        return phoneNumber.StartsWith("09") && phoneNumber.All(char.IsDigit);
    }
}