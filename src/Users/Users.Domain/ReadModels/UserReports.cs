using UserService.Users.Domain.Enums;

namespace UserService.Users.Domain.ReadModels;

public class UserReports
{
    public long ReportId { get; set; }
    public ReportStatus Status { get; set; }
    public DateTimeOffset ReportedAt { get; set; }
}