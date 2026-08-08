namespace NotificationService.Services.EmailService;

public class EmailSenderOptions
{
    public string SmtpHost { get; set; } = null!;
    public int SmtpPort { get; set; }
    public string SenderEmail { get; set; } = null!;
    public string SenderName { get; set; } = null!;
    public string AppPassword { get; set; } = null!;
}