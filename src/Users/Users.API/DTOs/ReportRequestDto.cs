using UserService.Users.Domain.Enums;

namespace UserService.Users.API.DTOs;

public record ReportRequestDto
{
    public string RequestConent { get; set; }
    public string Type { get; set; }
}