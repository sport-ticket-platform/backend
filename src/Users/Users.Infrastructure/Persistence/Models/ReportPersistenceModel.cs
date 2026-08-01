using UserService.Users.Domain.Enums;

namespace UserService.Users.Infrastructure.Persistence.Models;

public class ReportPersistenceModel
{
    public long ReportId { get; set; }
    public long UserId { get; set; }
    public ReportType Type { get; set; }
    public DateTimeOffset ReportedAt { get; set; }
    public string Request { get; set; }
    public string? Response { get; set; }
    public DateTimeOffset? RespondedAt { get; set; }
    public ReportStatus Status { get; set; }
}