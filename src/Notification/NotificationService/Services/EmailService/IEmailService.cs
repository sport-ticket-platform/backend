namespace NotificationService.Services.EmailService;

public interface IEmailService
{
    public Task SendTextEmail(string to, string subject, string textBody, CancellationToken ct = default);
    public Task SendHtmlEmail(string to, string subject, string htmlBody, CancellationToken ct = default);
}