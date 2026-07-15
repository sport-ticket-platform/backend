namespace UserService.Users.Application.Results;

public record GetUserResult(string FirstName, string LastName,string Email, string PhoneNumber);