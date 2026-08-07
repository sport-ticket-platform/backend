using System.Net.Http.Headers;
using System.Text;
using NotificationService.Exceptions;

namespace NotificationService.Services.EmailService;

public class EmailService : IEmailService
{
    private readonly ILogger<EmailService> _logger;
    private readonly IConfiguration _configuration;
    private readonly HttpClient _httpClient;
    private const string EmailServerUrl = "https://api.eu.mailgun.net/v3/";
    private const string DomainName = "sport-ticket-platform.com";

    public EmailService(ILogger<EmailService> logger, IConfiguration configuration, HttpClient httpClient)
    {
        _logger = logger;
        _configuration = configuration;
        _httpClient = httpClient;
    }

    public async Task SendTextEmail(string from, string to, string subject, string textBody)
    {
        var postData = new MultipartFormDataContent
        {
            { new StringContent(from), "from" },
            { new StringContent(to), "to" },
            { new StringContent(subject), "subject" },
            { new StringContent(textBody), "text" }
        };

        await SendAsync(from, to, postData);
    }

    public async Task SendHtmlEmail(string from, string to, string subject, string htmlBody)
    {
        var postData = new MultipartFormDataContent
        {
            { new StringContent(from), "from" },
            { new StringContent(to), "to" },
            { new StringContent(subject), "subject" },
            { new StringContent(htmlBody), "html" }
        };

        await SendAsync(from, to, postData);
    }

    private async Task SendAsync(string from, string to, MultipartFormDataContent postData)
    {
        _logger.LogInformation("Sending email from {From} to {To}", from, to);

        try
        {
            var base64String = Convert.ToBase64String(Encoding.ASCII.GetBytes($"api:{_configuration["APIKey"]}"));
            _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", base64String);

            using var response = await _httpClient.PostAsync(EmailServerUrl + DomainName + "/messages", postData);
            var responseBody = await response.Content.ReadAsStringAsync();

            if (!response.IsSuccessStatusCode)
            {
                _logger.LogError(
                    "Mailgun rejected the email from {From} to {To}. Status: {StatusCode}. Body: {Body}",
                    from, to, (int)response.StatusCode, responseBody);

                throw new EmailDeliveryException(
                    $"Email provider rejected the request with status {(int)response.StatusCode}.");
            }

            _logger.LogInformation("Email sent successfully from {From} to {To}", from, to);
        }
        catch (HttpRequestException ex)
        {
            _logger.LogError(ex, "Network error while sending email from {From} to {To}", from, to);
            throw new EmailDeliveryException("Unable to reach the email provider.", ex);
        }
        catch (TaskCanceledException ex) when (!ex.CancellationToken.IsCancellationRequested)
        {
            _logger.LogError(ex, "Email request to provider timed out from {From} to {To}", from, to);
            throw new EmailDeliveryException("Email provider did not respond in time.", ex);
        }
    }
}