using System.Net.Sockets;
using MailKit.Net.Smtp;
using MailKit.Security;
using Microsoft.Extensions.Options;
using MimeKit;
using NotificationService.Exceptions;

namespace NotificationService.Services.EmailService;

public class EmailService : IEmailService
{
    private readonly ILogger<EmailService> _logger;
    private readonly EmailSenderOptions _options;

    public EmailService(ILogger<EmailService> logger, IOptions<EmailSenderOptions> options)
    {
        _logger = logger;
        _options = options.Value;
    }

    public async Task SendTextEmail(string to, string subject, string textBody, CancellationToken ct = default)
    {
        await SendAsync(to, subject, textBody, isHtml: false, ct);
    }

    public async Task SendHtmlEmail(string to, string subject, string htmlBody, CancellationToken ct = default)
    {
        await SendAsync(to, subject, htmlBody, isHtml: true, ct);
    }

    private async Task SendAsync(string to, string subject, string body, bool isHtml, CancellationToken ct)
    {
        _logger.LogInformation("Sending email from {From} to {To}", _options.SenderEmail, to);

        var message = new MimeMessage();
        message.From.Add(new MailboxAddress(_options.SenderName, _options.SenderEmail));
        message.To.Add(MailboxAddress.Parse(to));
        message.Subject = subject;
        message.Body = isHtml
            ? new TextPart("html") { Text = body }
            : new TextPart("plain") { Text = body };

        using var client = new SmtpClient();

        try
        {
            await client.ConnectAsync(_options.SmtpHost, _options.SmtpPort, SecureSocketOptions.StartTls, ct);
            await client.AuthenticateAsync(_options.SenderEmail, _options.AppPassword, ct);
            await client.SendAsync(message, ct);

            _logger.LogInformation("Email sent to {To}", to);
        }
        catch (AuthenticationException ex)
        {
            _logger.LogError(ex, "Gmail SMTP authentication failed for sender {Sender}", _options.SenderEmail);
            throw new EmailDeliveryException("Failed to authenticate with the email provider.", ex);
        }
        catch (SmtpCommandException ex)
        {
            _logger.LogError(ex, "SMTP server rejected the email to {To}. Status: {StatusCode}", to, ex.StatusCode);
            throw new EmailDeliveryException($"Email provider rejected the request: {ex.Message}", ex);
        }
        catch (SmtpProtocolException ex)
        {
            _logger.LogError(ex, "SMTP protocol error while sending email to {To}", to);
            throw new EmailDeliveryException("Unexpected error communicating with the email provider.", ex);
        }
        catch (SocketException ex)
        {
            _logger.LogError(ex, "Network error while connecting to SMTP server for {To}", to);
            throw new EmailDeliveryException("Unable to reach the email provider.", ex);
        }
        catch (OperationCanceledException ex) when (!ct.IsCancellationRequested)
        {
            _logger.LogError(ex, "SMTP operation timed out while sending email to {To}", to);
            throw new EmailDeliveryException("Email provider did not respond in time.", ex);
        }
        finally
        {
            if (client.IsConnected)
                await client.DisconnectAsync(true, CancellationToken.None);
        }
    }
}