using Google.Protobuf.WellKnownTypes;
using Grpc.Core;
using NotificationService.Grpc;
using NotificationService.Services.EmailService;

namespace NotificationService.Services.GrpcService;

public class NotificationService : Grpc.NotificationService.NotificationServiceBase
{
    private readonly ILogger<NotificationService> _logger;
    private readonly IEmailService _emailService;

    public NotificationService(ILogger<NotificationService> logger, IEmailService emailService)
    {
        _logger = logger;
        _emailService = emailService;
    }

    public override async Task<Empty> SendHtmlEmail(SendEmailRequest request, ServerCallContext context)
    {
        _logger.LogInformation("sending emails to {to}", request.To);
        await _emailService.SendHtmlEmail(request.To, request.Subject, request.Body, context.CancellationToken);
        return new Empty();
    }

    public override async Task<Empty> SendTextEmail(SendEmailRequest request, ServerCallContext context)
    {
        _logger.LogInformation("sending emails to {to}", request.To);
        await _emailService.SendTextEmail(request.To, request.Subject, request.Body, context.CancellationToken);
        return new Empty();
    }
}