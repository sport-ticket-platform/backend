namespace UserService.Users.API.DTOs;

public record UserProfileDto(
    string FirstName,
    string LastName,
    string Email,
    string? PhoneNumber,
    string? City);