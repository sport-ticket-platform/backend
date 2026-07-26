using UserService.Users.Domain.Enums;

namespace UserService.Users.API.DTOs;

public record ReportDto
{
    public string RequestConent { get; set; }
    public ReportType Type { get; set; }
}