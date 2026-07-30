using UserService.Users.Domain.Enums;

namespace UserService.Users.Infrastructure.Persistence.Models;

public class UserPersistenceModel
{
    public long UserId { get; set; }
    public string Firstname { get; set; }
    public string LastName { get; set; }
    public Role Role { get; set; }
    public string email { get; set; }
    public bool IsEmailVerified { get; set; }
    public string PhoneNumber { get; set; }
    public bool IsPhoneNumberVerified { get; set; }
    public DateTimeOffset RegistrationDate { get; set; }
    public string PasswordHash { get; set; }
    public decimal Balance { get; private set; } 
    public int CityId { get; private set; }
    public bool IsActive { get; private set; }
    public bool IsTwoFactorEnabled { get; set; }
    
    
    
    public UserPersistenceModel(long userId, string firstname, string lastName, Role role, string email, bool isEmailVerified, string phoneNumber, bool isPhoneNumberVerified, DateTimeOffset registrationDate, string passwordHash, decimal balance, int cityId, bool isActive, bool isTwoFactorEnabled)
    {
        UserId = userId;
        Firstname = firstname;
        LastName = lastName;
        Role = role;
        this.email = email;
        IsEmailVerified = isEmailVerified;
        PhoneNumber = phoneNumber;
        IsPhoneNumberVerified = isPhoneNumberVerified;
        RegistrationDate = registrationDate;
        PasswordHash = passwordHash;
        Balance = balance;
        CityId = cityId;
        IsActive = isActive;
        IsTwoFactorEnabled = isTwoFactorEnabled;
    }


    
}