using UserService.Users.Domain.Enums;

namespace UserService.Users.Infrastructure.Persistence.Models;

public class ReportPersistenceModel
{

    public long ReportId { get; private set; }
    public long UserId { get; private set; }
    public ReportType Type { get; private set; }
    public DateTimeOffset ReportedAt { get; private set; }
    public string Request { get; private set; }
    public string? Response { get; private set; }
    public DateTimeOffset? RespondedAt { get; private set; }
    public ReportStatus Status { get; private set; }
    
    
    public ReportPersistenceModel(long reportId, long userId, ReportType type, DateTimeOffset reportedAt, string request, string? response, DateTimeOffset? respondedAt, ReportStatus status)
    {
        ReportId = reportId;
        UserId = userId;
        Type = type;
        ReportedAt = reportedAt;
        Request = request;
        Response = response;
        RespondedAt = respondedAt;
        Status = status;
    }


}