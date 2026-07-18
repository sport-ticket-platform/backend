using System.Text.RegularExpressions;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Exceptions;

namespace UserService.Users.Domain.Models;

public class User
{
    public long UserId { get; private set; }
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
    public bool IsActive { get; private set; } = true;

    public bool IsTwoFactorEnabled { get; private set; } = false;

    private static readonly Regex EmailRegex = new(
        @"^[^@\s]+@[^@\s]+\.[^@\s]+$",
        RegexOptions.Compiled);

    
    private const int LeastChars = 5;
    private const int MostChars = 50;
    private const string PhoneNumberPrefix = "09";
    private const int PhoneNumberLength = 11;
    private const int CurrencyDecimalPlaces = 2;
    


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
    
    
    private static void ValidateMoneyAmount(decimal amount)
    {
        if (amount <= 0)
            throw new DomainException("Amount must be greater than zero.");

        if (amount != Math.Round(amount, CurrencyDecimalPlaces))
            throw new DomainException($"Amount cannot have more than {CurrencyDecimalPlaces} decimal places.");
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

    
    public void Update(string firstName, string lastName,string email,string phoneNumber,int cityId)
    {
        if (cityId <= 0)
            throw new DomainException("The city Id cannot be negative.");

        ValidateNameLength(firstName,nameof(firstName));
        ValidateNameLength(lastName,nameof(lastName));
        ValidateEmail(email);
        ValidatePhoneNumber(phoneNumber);
        
        FirstName = firstName;
        LastName = lastName;
        Email = email;
        PhoneNumber = phoneNumber;
        CityId = cityId;
    }

    
    public void Update(string firstName, string lastName,string email)
    {
        ValidateNameLength(firstName,nameof(firstName));
        ValidateNameLength(lastName,nameof(lastName));
        ValidateEmail(email);
        FirstName = firstName;
        LastName = lastName;
        Email = email;
    }

    public void Update(string firstName, string lastName)
    {
        ValidateNameLength(firstName,nameof(firstName));
        ValidateNameLength(lastName,nameof(lastName));
        FirstName = firstName;
        LastName = lastName;
    }
    public void Update(string phoneNumber)
    {
        ValidatePhoneNumber(phoneNumber);
        PhoneNumber = phoneNumber;
    }

    public void IncreaseBalance(decimal amount)
    {
        ValidateMoneyAmount(amount);
        Balance += amount;
    }

    public void DecreaseBalance(decimal amount)
    {
        ValidateMoneyAmount(amount);

        if (amount > Balance)
            throw new DomainException("Insufficient balance.");

        Balance -= amount;
    }

    public void DeactivateAccount() => IsActive = false;
    
    public void ActivateAccount() => IsActive = true;

    public void EnableTwoFactor() => IsTwoFactorEnabled = true;

    public void DisableTwoFactor() => IsTwoFactorEnabled = false;


}