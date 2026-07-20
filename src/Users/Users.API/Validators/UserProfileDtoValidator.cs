using FluentValidation;
using UserService.Users.API.DTOs;

namespace UserService.Users.API.Validators;

public class UserProfileDtoValidator : AbstractValidator<UserProfileDto>
{
    public UserProfileDtoValidator()
    {
        RuleFor(userProfile => userProfile.UserId)
            .NotEmpty()
            .GreaterThan(0);

        RuleFor(userProfile => userProfile.FirstName)
            .NotEmpty()
            .NotEqual(userProfile => userProfile.LastName)
            .MinimumLength(5)
            .MaximumLength(50);

        RuleFor(userProfile => userProfile.LastName)
            .NotEmpty()
            .NotEqual(userProfile => userProfile.FirstName)
            .MinimumLength(5)
            .MaximumLength(50);

        RuleFor(userProfile => userProfile.Email).EmailAddress();

        RuleFor(userProfile => userProfile.PhoneNumber)
            .NotEmpty()
            .Length(11)
            .Must(ValidatePhoneNumber).WithMessage("The phone number must start with \"09\" and all should be digits");
        
        RuleFor(userProfile => userProfile.City)
            .NotEmpty();
    }

    private bool ValidatePhoneNumber(string phoneNumber)
    {
        return phoneNumber.StartsWith("09") && phoneNumber.All(char.IsDigit);
    }
}