namespace UserService.Users.Application.Requests;

public record UpdateProfileRequest(long UserId,string FirstName, string LastName,string Email, string PhoneNumber,string City);