using UserService.Users.Domain.Enums;

namespace UserService.Users.Infrastructure.Persistence.Models;

public class UserPersistenceModel
{
    public long UserId { get; set; }
    public string Firstname { get; set; }
    public string LastName { get; set; }
    public Role Role { get; set; }
    public string Email { get; set; }
    public bool IsEmailVerified { get; set; }
    public string? PhoneNumber { get; set; }
    public bool IsPhoneNumberVerified { get; set; }
    public DateTimeOffset RegistrationDate { get; set; }
    public string PasswordHash { get; set; }
    public decimal Balance { get; set; } 
    public int? CityId { get; set; }
    public bool IsActive { get; set; }
    public bool IsTwoFactorEnabled { get; set; }
}