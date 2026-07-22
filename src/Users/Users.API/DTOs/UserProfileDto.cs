namespace UserService.Users.API.DTOs;

public record UserProfileDto(
    long UserId,
    string FirstName,
    string LastName,
    string Email,
    string PhoneNumber,
    string City);