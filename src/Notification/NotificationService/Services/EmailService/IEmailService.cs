namespace NotificationService.Services.EmailService;

public interface IEmailService
{
    public Task SendTextEmail(string from, string to, string subject, string textBody);
    public Task SendHtmlEmail(string from, string to, string subject, string htmlBody);

}