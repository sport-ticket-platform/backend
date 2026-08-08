using NotificationService.Middlewares;
using NotificationService.Services.EmailService;
using Resend;

var builder = WebApplication.CreateBuilder(args);


builder.Services.AddScoped<IEmailService, EmailService>();
builder.Services.Configure<EmailSenderOptions>(builder.Configuration.GetSection("Gmail"));

var app = builder.Build();

app.MapGet("/email", async (IEmailService emailService, CancellationToken ct) =>
{
    await emailService.SendTextEmail("mohammadBahadori1384@gmail.com",
        "greeting", "hello bitch", ct);
});

app.UseMiddleware<ExceptionHandlingMiddleware>();
app.Run();