using UserService.Users.Domain.Enums;

namespace UserService.Users.API.DTOs;

public record ReportRequestDto
{
    public string RequestContent { get; set; }
    public string Type { get; set; }
}