using FluentValidation;
using UserService.Users.API.DTOs;

namespace UserService.Users.API.Validators;

public class UserProfileDtoValidator : AbstractValidator<UserProfileDto>
{
    public UserProfileDtoValidator()
    {
        RuleFor(userProfile => userProfile.FirstName)
            .NotNull()
            .NotEqual(userProfile => userProfile.LastName)
            .MinimumLength(5)
            .MaximumLength(50);

        RuleFor(userProfile => userProfile.LastName)
            .NotNull()
            .NotEqual(userProfile => userProfile.FirstName)
            .MinimumLength(5)
            .MaximumLength(50);

        RuleFor(userProfile => userProfile.Email).EmailAddress();

        RuleFor(userProfile => userProfile.PhoneNumber)
            .Empty()
            .Length(11)
            .Must(ValidatePhoneNumber).WithMessage("The phone number must start with \"09\" and all should be digits");
    }

    private bool ValidatePhoneNumber(string phoneNumber)
    {
        return phoneNumber.StartsWith("09") && phoneNumber.All(char.IsDigit);
    }
}