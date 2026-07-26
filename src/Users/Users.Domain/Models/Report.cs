
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Exceptions;

namespace UserService.Users.Domain.Models;

public class Report
{
    private const int MaxRequestLength = 500;
    private const int MaxResponseLength = 500;

    public long ReportId { get; private set; }
    public long UserId { get; private set; }
    public ReportType Type { get; private set; }
    public DateTimeOffset ReportedAt { get; private set; }
    public string Request { get; private set; }
    public string? Response { get; private set; }
    public DateTimeOffset? RespondedAt { get; private set; }
    public ReportStatus Status { get; private set; }

    private Report(
        long userId,
        ReportType type,
        DateTimeOffset reportedAt,
        string request,
        string? response,
        DateTimeOffset? respondedAt,
        ReportStatus status)
    {
        UserId = userId;
        Type = type;
        ReportedAt = reportedAt;
        Request = request;
        Response = response;
        RespondedAt = respondedAt;
        Status = status;
    }

    public static Report Create(long userId, ReportType type, string request)
    {
        ValidateId(userId, nameof(userId));
        ValidateRequest(request);

        return new Report(
            userId,
            type,
            DateTimeOffset.Now,
            request,
            response: null,
            respondedAt: null,
            status: ReportStatus.OPEN);
    }

    
    public void AnswerReport(string response, DateTimeOffset respondedAt)
    {
        if (Status != ReportStatus.OPEN)
            throw new DomainException("A report must be OPENED before it can be CLOSED.");
        
        ValidateResponse(response);
        
        if (respondedAt.CompareTo(ReportedAt) <= 0)
            throw new ArgumentException("The report time  cannot be equal of after the respond time");

        Response = response;
        RespondedAt = respondedAt;

    }
    
    private static void ValidateId(long id, string name)
    {
        if (id <= 0)
            throw new DomainException($"The {name} must be positive.");
    }

    private static void ValidateRequest(string request)
    {
        if (string.IsNullOrWhiteSpace(request))
            throw new DomainException("The report request text cannot be empty.");

        if (request.Length > MaxRequestLength)
            throw new DomainException($"The report request text cannot exceed {MaxRequestLength} characters.");
    }

    private static void ValidateResponse(string response)
    {
        if (string.IsNullOrWhiteSpace(response))
            throw new DomainException("The response text cannot be empty.");

        if (response.Length > MaxResponseLength)
            throw new DomainException($"The response text cannot exceed {MaxResponseLength} characters.");
    }
}