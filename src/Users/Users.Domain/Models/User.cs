using System.Text.RegularExpressions;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Exceptions;

namespace UserService.Users.Domain.Models;

public class User
{
    public Int64 UserId { get; private set; }
    public string FirstName { get; private set; }
    public string LastName { get; private set; }
    public Role UserRole { get; private set; }
    public string Email { get; private set; }
    public bool EmailIsVerified { get; private set; } = false;
    public string PhoneNumber { get; private set; }
    public bool PhoneNumberIsVerified { get; private set; } = false;
    public DateTimeOffset RegistrationDate { get; private set; } = DateTimeOffset.Now;
    public string PasswordHash { get; private set; }
    public decimal Balance { get; private set; } = 0;
    public int CityId { get; private set; }
    public bool AccountStatus { get; private set; }

    private static readonly Regex EmailRegex = new(
        @"^[^@\s]+@[^@\s]+\.[^@\s]+$",
        RegexOptions.Compiled);

    
    private const int LeastChars = 5;
    private const int MostChars = 50;
    private const string PhoneNumberPrefix = "09";
    private const int PhoneNumberLength = 11;


    private User(string firstName, string lastName, Role userRole, string email, string phoneNumber,
        string passwordHash, int cityId, bool emailIsVerified = false, bool phoneNumberIsVerified = false)
    {
        FirstName = firstName;
        LastName = lastName;
        UserRole = userRole;
        Email = email;
        PhoneNumber = phoneNumber;
        PasswordHash = passwordHash;
        CityId = cityId;
        EmailIsVerified = emailIsVerified;
        PhoneNumberIsVerified = phoneNumberIsVerified;
    }

    
    private static void ValidateNameLength(string value, string fieldName)
    {
        if (string.IsNullOrWhiteSpace(value) || value.Length < LeastChars || value.Length > MostChars)
            throw new DomainException(
                $"{fieldName} must be between {LeastChars} and {MostChars} characters.");
    }

    private static void ValidatePhoneNumber(string phoneNumber)
    {
        if (string.IsNullOrWhiteSpace(phoneNumber))
            throw new DomainException("Phone number is required.");

        if (phoneNumber.Length != PhoneNumberLength)
            throw new DomainException($"Phone number must be exactly {PhoneNumberLength} digits.");

        if (!phoneNumber.StartsWith(PhoneNumberPrefix))
            throw new DomainException($"Phone number must start with '{PhoneNumberPrefix}'.");

        if (!phoneNumber.All(char.IsDigit))
            throw new DomainException("Phone number must contain digits only.");
    }
    
    
    private static void ValidateEmail(string email)
    {
        if (string.IsNullOrWhiteSpace(email))
            throw new DomainException("Email is required.");

        if (!EmailRegex.IsMatch(email))
            throw new DomainException("Email format is invalid.");
    }
    public static User Create(string firstName, string lastName, Role userRole, string email, string phoneNumber,
        string passwordHash, int cityId, bool emailIsVerified = false, bool phoneNumberIsVerified = false)
    {
        ValidateNameLength(firstName, nameof(firstName));
        ValidateNameLength(lastName, nameof(lastName));
        ValidateEmail(email);
        ValidatePhoneNumber(phoneNumber);
        
        if (string.IsNullOrEmpty(passwordHash))
            throw new DomainException("The password is required.");
        
        if(cityId <= 0)
            throw new DomainException("The city id must be positive.");
        
        return new User(firstName, lastName, userRole, email, phoneNumber, passwordHash, cityId, emailIsVerified,
            phoneNumberIsVerified);
    }
}