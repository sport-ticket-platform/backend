namespace UserService.Users.API.DTOs;

public class UserFilterDto
{
    public string? FirstName { get; set; }
    public string? LastName { get; set; }
    public string? Email { get; set; }
    public bool? Status { get; set; }

    public int Limit { get; set; } = 20;
    public int Offset { get; set; } = 0;
}