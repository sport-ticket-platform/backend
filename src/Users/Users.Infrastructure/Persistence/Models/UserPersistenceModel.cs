namespace UserService.Users.Infrastructure.Persistence.Models;

public class UserPersistenceModel
{
    public long UserId { get; set; }
    public string Firstname { get; set; }
    public string LastName { get; set; }
    public string Role { get; set; }
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
}